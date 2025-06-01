package com.taskapp.service;

import com.taskapp.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Batches notifications to reduce SSE event frequency and improve performance.
 * Collects multiple rapid-fire notifications and sends them in batches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationBatchingService {
    
    private final SSEConnectionManager sseConnectionManager;
    private final MessagePublisher messagePublisher;
    private final MessagingProperties messagingProperties;
    
    // Queue of pending notifications per user
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<NotificationEvent>> pendingNotifications = new ConcurrentHashMap<>();
    
    // Batch size and timing configuration
    private static final int MAX_BATCH_SIZE = 5;
    private static final long BATCH_TIMEOUT_MS = 100; // 100ms batching window
    
    /**
     * Add notification to batch queue for a user
     */
    public void queueNotification(String username, NotificationEvent notification) {
        if (username == null || notification == null) {
            log.warn("Skipping null notification or username");
            return;
        }
        
        pendingNotifications.computeIfAbsent(username, k -> new ConcurrentLinkedQueue<>())
                           .offer(notification);
        
        // Check if we should flush immediately (batch size reached)
        ConcurrentLinkedQueue<NotificationEvent> userQueue = pendingNotifications.get(username);
        if (userQueue != null && userQueue.size() >= MAX_BATCH_SIZE) {
            flushUserNotifications(username);
        }
    }
    
    /**
     * Flush all pending notifications for all users (scheduled every 100ms)
     */
    @Scheduled(fixedDelay = BATCH_TIMEOUT_MS)
    public void flushAllPendingNotifications() {
        if (pendingNotifications.isEmpty()) {
            return;
        }
        
        log.debug("Flushing pending notifications for {} users", pendingNotifications.size());
        
        // Process each user's notifications
        List<String> usersToFlush = new ArrayList<>(pendingNotifications.keySet());
        for (String username : usersToFlush) {
            flushUserNotifications(username);
        }
    }
    
    /**
     * Flush notifications for a specific user
     */
    private void flushUserNotifications(String username) {
        ConcurrentLinkedQueue<NotificationEvent> userQueue = pendingNotifications.get(username);
        if (userQueue == null || userQueue.isEmpty()) {
            return;
        }
        
        // Extract all notifications for this user
        List<NotificationEvent> notificationsToSend = new ArrayList<>();
        NotificationEvent notification;
        while ((notification = userQueue.poll()) != null) {
            notificationsToSend.add(notification);
        }
        
        // Remove empty queue
        if (userQueue.isEmpty()) {
            pendingNotifications.remove(username);
        }
        
        // Send notifications in order
        if (!notificationsToSend.isEmpty()) {
            log.debug("Sending batch of {} notifications to user: {}", notificationsToSend.size(), username);
            
            for (NotificationEvent notif : notificationsToSend) {
                sseConnectionManager.sendToUserLocal(username, notif);
            }
        }
    }
    
    /**
     * Get pending notification count for monitoring
     */
    public int getPendingNotificationCount() {
        return pendingNotifications.values().stream()
                .mapToInt(ConcurrentLinkedQueue::size)
                .sum();
    }
    
    /**
     * Get pending notification count for a specific user
     */
    public int getPendingNotificationCount(String username) {
        ConcurrentLinkedQueue<NotificationEvent> userQueue = pendingNotifications.get(username);
        return userQueue != null ? userQueue.size() : 0;
    }
}
