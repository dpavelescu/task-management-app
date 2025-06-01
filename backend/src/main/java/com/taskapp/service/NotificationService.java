package com.taskapp.service;

import com.taskapp.dto.NotificationEvent;
import com.taskapp.messaging.MessagePublisher;
import com.taskapp.messaging.config.MessagingProperties;
import com.taskapp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Notification service for sending real-time notifications via Redis pub/sub.
 * Handles cross-pod notification delivery in multi-instance deployments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final MessagePublisher messagePublisher;
    private final MessagingProperties messagingProperties;
    private final SSEConnectionManager sseConnectionManager;

    /**
     * Send notification to user with cross-pod distribution via pub/sub
     */
    public void sendNotification(NotificationEvent notification) {
        if (notification == null) {
            log.error("Cannot send null notification");
            return;
        }
        
        String username = notification.getUsername();
        if (username == null || username.trim().isEmpty()) {
            log.error("Cannot send notification without username: {}", notification);
            return;
        }
        
        log.debug("Publishing notification for user {}: {} (id: {})", 
                 username, notification.getType(), notification.getId());
          
        try {
            Map<String, String> attributes = Map.of(
                "username", username,
                "type", notification.getType(),
                "podId", messagingProperties.getPodId()
            );
            
            messagePublisher.publishMessage(
                messagingProperties.getTopics().getUserNotifications(),
                notification,
                attributes
            );
            
            log.debug("Successfully published notification to Redis");
                     
        } catch (Exception e) {
            log.error("Failed to publish notification for user {}: {}", username, e.getMessage(), e);
            
            // Fallback: try direct local delivery if pub/sub fails
            log.info("Attempting fallback local delivery for user: {}", username);
            sseConnectionManager.sendToUserLocal(username, notification);
        }
    }

    /**
     * Create SSE connection for user
     */
    public SseEmitter createConnection(String token, String lastEventId) {
        // Validate token and extract username
        String username = jwtTokenProvider.getUsernameFromToken(token);
        if (username == null) {
            log.error("Invalid JWT token provided");
            throw new IllegalArgumentException("Invalid or expired token");
        }
        
        log.debug("Creating SSE connection for user: {} (lastEventId: {})", username, lastEventId);
        
        // Register connection with manager
        return sseConnectionManager.createConnection(username, lastEventId);
    }

    /**
     * Get active connection count for monitoring
     */
    public int getActiveConnectionCount() {
        return sseConnectionManager.getActiveConnectionCount();
    }

    /**
     * Get recent notification count for user
     */
    public int getRecentNotificationCount(String username) {
        return sseConnectionManager.getRecentNotificationCount(username);
    }

    /**
     * Check if the messaging system is healthy
     */
    public boolean isMessagingHealthy() {
        try {
            return messagePublisher.isHealthy();
        } catch (Exception e) {
            log.warn("Messaging health check failed: {}", e.getMessage());
            return false;
        }
    }
}
