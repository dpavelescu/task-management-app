package com.taskapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskapp.dto.NotificationEvent;
import com.taskapp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
      // Store SSE connections by username
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> userConnections = new ConcurrentHashMap<>();
    
    // Store recent notifications for Last-Event-ID support (keep last 100 per user)
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<NotificationEvent>> recentNotifications = new ConcurrentHashMap<>();
    private static final int MAX_RECENT_NOTIFICATIONS = 100;
    
    public SseEmitter createConnection(String token, String lastEventId) {
        try {
            // Validate token and extract username
            String username = jwtTokenProvider.getUsernameFromToken(token);
            if (username == null) {
                log.error("Invalid JWT token provided");
                throw new IllegalArgumentException("Invalid or expired token");
            }
            
            log.debug("Creating SSE connection for user: {} (lastEventId: {})", username, lastEventId);
            
            // Create SSE emitter with standard timeout - browser will auto-reconnect
            SseEmitter emitter = new SseEmitter(0L); // 0 = no timeout, let browser handle reconnection
            
            // Add to user connections
            userConnections.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>()).add(emitter);
            
            // Send missed notifications since lastEventId
            if (lastEventId != null && !lastEventId.trim().isEmpty()) {
                sendNotificationsSince(username, emitter, lastEventId);
            }
            
            // Handle completion and cleanup
            emitter.onCompletion(() -> {
                log.debug("SSE connection completed for user: {}", username);
                removeConnection(username, emitter);
            });
            
            emitter.onTimeout(() -> {
                log.debug("SSE connection timed out for user: {}", username);
                removeConnection(username, emitter);
            });
            
            emitter.onError((throwable) -> {
                log.debug("SSE connection error for user: {} - {}", username, throwable.getMessage());
                removeConnection(username, emitter);
            });
              log.info("SSE connection established for user: {} (active connections: {})", 
                username, userConnections.get(username).size());
            
            return emitter;
              } catch (Exception e) {
            log.error("Failed to create SSE connection", e);
            throw new RuntimeException("Failed to create SSE connection", e);
        }
    }
    
    private void sendNotificationsSince(String username, SseEmitter emitter, String lastEventId) {
        CopyOnWriteArrayList<NotificationEvent> userNotifications = recentNotifications.get(username);
        if (userNotifications == null || userNotifications.isEmpty()) {
            return;
        }
        
        log.debug("Checking for notifications since eventId: {} for user: {}", lastEventId, username);
        
        // Find notifications after the lastEventId
        boolean foundLastEvent = false;
        int sentCount = 0;
        
        for (NotificationEvent notification : userNotifications) {
            if (foundLastEvent) {
                if (sendToEmitter(emitter, notification)) {
                    sentCount++;
                } else {
                    break; // Stop if connection fails
                }
            } else if (lastEventId.equals(notification.getId())) {
                foundLastEvent = true;
            }        }
          if (sentCount > 0) {
            log.debug("Sent {} missed notifications to user: {}", sentCount, username);
        }
    }
    
    public void sendNotification(NotificationEvent notification) {
        if (notification == null) {
            log.error("Cannot send null notification");
            return;
        }
        
        String username = notification.getUsername();
        if (username == null || username.trim().isEmpty()) {
            log.error("Cannot send notification without username: {}", notification);
            return;
        }        log.debug("Sending notification to user {}: {} (id: {})", username, notification.getType(), notification.getId());
        
        // Store notification for Last-Event-ID support
        storeRecentNotification(username, notification);
          // Send to active connections
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(username);
        if (connections == null || connections.isEmpty()) {
            log.debug("No active SSE connections for user {}, notification stored for later delivery", username);
            return;
        }

        log.debug("Found {} active SSE connections for user {}", connections.size(), username);
          // Send to all active connections for this user
        int successCount = 0;
        int failureCount = 0;
        List<SseEmitter> failedEmitters = new ArrayList<>();
        
        for (SseEmitter emitter : connections) {
            if (sendToEmitter(emitter, notification)) {
                successCount++;
            } else {
                failureCount++;
                failedEmitters.add(emitter);
            }
        }
          // Remove failed connections
        failedEmitters.forEach(connections::remove);
        
        if (failureCount > 0) {
            log.debug("Notification delivery for user {}: {} successful, {} failed", 
                username, successCount, failureCount);
        }

        // Clean up empty connection list
        if (connections.isEmpty()) {
            userConnections.remove(username);
            log.debug("Removed empty connection list for user {}", username);
        }
    }
    
    private void storeRecentNotification(String username, NotificationEvent notification) {
        CopyOnWriteArrayList<NotificationEvent> userNotifications = 
            recentNotifications.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>());
          userNotifications.add(notification);
        
        // Keep only the most recent notifications
        if (userNotifications.size() > MAX_RECENT_NOTIFICATIONS) {
            userNotifications.subList(0, userNotifications.size() - MAX_RECENT_NOTIFICATIONS).clear();
        }
    }
    
    private boolean sendToEmitter(SseEmitter emitter, NotificationEvent notification) {
        try {
            // Create a simple data object to avoid circular references
            String jsonData = objectMapper.writeValueAsString(notification);
            log.debug("Sending SSE notification: {}", jsonData);
            
            // Validate that we have data to send
            if (jsonData == null || jsonData.trim().isEmpty() || jsonData.equals("{}")) {
                log.error("JSON data is empty or invalid for notification: {}", notification);
                return false;
            }
            
            // Send as SSE event with specific event type and ID for Last-Event-ID support
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .id(notification.getId())
                    .name("notification")
                    .data(jsonData);
              emitter.send(event);
            log.debug("Successfully sent SSE notification with id: {}", notification.getId());
            return true;
            
        } catch (IllegalStateException e) {
            // Connection was closed or recycled
            log.debug("SSE connection was closed for notification {}: {}", notification.getId(), e.getMessage());
            return false;
        } catch (IOException e) {
            // Network or connection issue
            log.debug("SSE connection IO error for notification {}: {}", notification.getId(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Failed to send notification via SSE: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private void removeConnection(String username, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(username);
        if (connections != null) {
            connections.remove(emitter);
            if (connections.isEmpty()) {
                userConnections.remove(username);
            }
        }
    }
    
    // Get connection count for monitoring
    public int getActiveConnectionCount() {
        return userConnections.values().stream()
                .mapToInt(CopyOnWriteArrayList::size)
                .sum();
    }
    
    public int getRecentNotificationCount(String username) {
        CopyOnWriteArrayList<NotificationEvent> recent = recentNotifications.get(username);
        return recent != null ? recent.size() : 0;
    }
}
