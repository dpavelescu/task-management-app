package com.taskapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskapp.dto.NotificationEvent;
import com.taskapp.messaging.config.MessagingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages SSE connections for this specific pod instance.
 * Integrates with pub/sub messaging for cross-pod notification delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SSEConnectionManager {
    
    private final ObjectMapper objectMapper;
    private final MessagingProperties messagingProperties;
    
    // Store SSE connections by username for this pod
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> userConnections = new ConcurrentHashMap<>();
    
    // Store recent notifications for Last-Event-ID support (keep last 100 per user)
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<NotificationEvent>> recentNotifications = new ConcurrentHashMap<>();
    private static final int MAX_RECENT_NOTIFICATIONS = 100;
    
    /**
     * Create a new SSE connection for a user
     */
    public SseEmitter createConnection(String username, String lastEventId) {
        log.debug("Creating SSE connection for user: {} on pod: {} (lastEventId: {})", 
                 username, messagingProperties.getPodId(), lastEventId);
        
        // Create SSE emitter with no timeout - let browser handle reconnection
        SseEmitter emitter = new SseEmitter(0L);
        
        // Add to user connections
        userConnections.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        // Send missed notifications since lastEventId
        if (lastEventId != null && !lastEventId.trim().isEmpty()) {
            sendNotificationsSince(username, emitter, lastEventId);
        }
        
        // Handle connection lifecycle events
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed for user: {} on pod: {}", username, messagingProperties.getPodId());
            removeConnection(username, emitter);
        });
        
        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out for user: {} on pod: {}", username, messagingProperties.getPodId());
            removeConnection(username, emitter);
        });
        
        emitter.onError((throwable) -> {
            log.debug("SSE connection error for user: {} on pod: {} - {}", 
                     username, messagingProperties.getPodId(), throwable.getMessage());
            removeConnection(username, emitter);
        });
        
        log.info("SSE connection established for user: {} on pod: {} (total connections: {})", 
                username, messagingProperties.getPodId(), userConnections.get(username).size());
        
        return emitter;
    }
    
    /**
     * Check if this pod has an active connection for the user
     */
    public boolean hasConnection(String username) {
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(username);
        return connections != null && !connections.isEmpty();
    }
    
    /**
     * Send notification to user if they have connections on this pod
     */
    public void sendToUserLocal(String username, NotificationEvent notification) {
        // Store notification for Last-Event-ID support
        storeRecentNotification(username, notification);
        
        // Send to active connections on this pod
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(username);
        if (connections == null || connections.isEmpty()) {
            log.debug("No active SSE connections for user {} on pod {}, notification stored for later delivery", 
                     username, messagingProperties.getPodId());
            return;
        }
        
        log.debug("Found {} active SSE connections for user {} on pod {}", 
                 connections.size(), username, messagingProperties.getPodId());
        
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
            log.debug("Notification delivery for user {} on pod {}: {} successful, {} failed", 
                     username, messagingProperties.getPodId(), successCount, failureCount);
        }
        
        // Clean up empty connection list
        if (connections.isEmpty()) {
            userConnections.remove(username);
            log.debug("Removed empty connection list for user {} on pod {}", username, messagingProperties.getPodId());
        }
    }
    
    /**
     * Send notifications since a specific event ID to a specific emitter
     */
    private void sendNotificationsSince(String username, SseEmitter emitter, String lastEventId) {
        CopyOnWriteArrayList<NotificationEvent> userNotifications = recentNotifications.get(username);
        if (userNotifications == null || userNotifications.isEmpty()) {
            return;
        }
        
        log.debug("Checking for notifications since eventId: {} for user: {} on pod: {}", 
                 lastEventId, username, messagingProperties.getPodId());
        
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
            }
        }
        
        if (sentCount > 0) {
            log.debug("Sent {} missed notifications to user: {} on pod: {}", 
                     sentCount, username, messagingProperties.getPodId());
        }
    }
    
    /**
     * Store notification for Last-Event-ID replay support
     */
    private void storeRecentNotification(String username, NotificationEvent notification) {
        CopyOnWriteArrayList<NotificationEvent> userNotifications = 
            recentNotifications.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>());
        
        userNotifications.add(notification);
        
        // Keep only the most recent notifications
        if (userNotifications.size() > MAX_RECENT_NOTIFICATIONS) {
            userNotifications.subList(0, userNotifications.size() - MAX_RECENT_NOTIFICATIONS).clear();
        }
    }
    
    /**
     * Send notification to a specific SSE emitter
     */
    private boolean sendToEmitter(SseEmitter emitter, NotificationEvent notification) {
        try {
            // Create JSON data
            String jsonData = objectMapper.writeValueAsString(notification);
            log.debug("Sending SSE notification on pod {}: {}", messagingProperties.getPodId(), jsonData);
            
            // Validate data
            if (jsonData == null || jsonData.trim().isEmpty() || jsonData.equals("{}")) {
                log.error("JSON data is empty or invalid for notification: {}", notification);
                return false;
            }
            
            // Send as SSE event with ID for Last-Event-ID support
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .id(notification.getId())
                    .name("notification")
                    .data(jsonData);
            
            emitter.send(event);
            log.debug("Successfully sent SSE notification with id: {} on pod: {}", 
                     notification.getId(), messagingProperties.getPodId());
            return true;
            
        } catch (IllegalStateException e) {
            // Connection was closed or recycled
            log.debug("SSE connection was closed for notification {} on pod {}: {}", 
                     notification.getId(), messagingProperties.getPodId(), e.getMessage());
            return false;
        } catch (IOException e) {
            // Network or connection issue
            log.debug("SSE connection IO error for notification {} on pod {}: {}", 
                     notification.getId(), messagingProperties.getPodId(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Failed to send notification via SSE on pod {}: {}", 
                     messagingProperties.getPodId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Remove a specific connection
     */
    private void removeConnection(String username, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(username);
        if (connections != null) {
            connections.remove(emitter);
            if (connections.isEmpty()) {
                userConnections.remove(username);
            }
        }
    }
    
    /**
     * Get total active connection count for this pod
     */
    public int getActiveConnectionCount() {
        return userConnections.values().stream()
                .mapToInt(CopyOnWriteArrayList::size)
                .sum();
    }
    
    /**
     * Get recent notification count for a specific user
     */
    public int getRecentNotificationCount(String username) {
        CopyOnWriteArrayList<NotificationEvent> recent = recentNotifications.get(username);
        return recent != null ? recent.size() : 0;
    }
    
    /**
     * Get usernames connected to this pod
     */
    public Set<String> getConnectedUsers() {
        return userConnections.keySet();
    }
    
    /**
     * Clean up stale connections (called by health monitor)
     */
    public void removeStaleConnections() {
        userConnections.entrySet().removeIf(entry -> {
            String username = entry.getKey();
            CopyOnWriteArrayList<SseEmitter> connections = entry.getValue();
            
            // Remove any null or completed connections
            connections.removeIf(emitter -> emitter == null);
            
            boolean isEmpty = connections.isEmpty();
            if (isEmpty) {
                log.debug("Removed stale connection entry for user: {} on pod: {}", 
                         username, messagingProperties.getPodId());
            }
            return isEmpty;
        });
    }
}
