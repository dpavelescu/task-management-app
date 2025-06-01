package com.taskapp.messaging.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskapp.dto.NotificationEvent;
import com.taskapp.service.SSEConnectionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis message consumer that receives notifications from Redis pub/sub
 * and forwards them to local SSE connections via SSEConnectionManager.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageConsumer implements MessageListener {
    
    private final SSEConnectionManager sseConnectionManager;
    private final ObjectMapper objectMapper;
    
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String messageBody = new String(message.getBody());
            String channel = new String(message.getChannel());
            
            log.debug("Received Redis message on channel {}: {}", channel, messageBody);
            
            // Parse the notification event
            NotificationEvent notification = objectMapper.readValue(messageBody, NotificationEvent.class);
            
            // Forward to SSE connection manager for local delivery
            String username = notification.getUsername();
            if (username != null && !username.trim().isEmpty()) {
                sseConnectionManager.sendToUserLocal(username, notification);
                log.debug("Forwarded Redis notification to local SSE connections for user: {}", username);
            } else {
                log.warn("Received notification without username, skipping: {}", notification);
            }
            
        } catch (Exception e) {
            log.error("Error processing Redis message: {}", e.getMessage(), e);
        }
    }
}
