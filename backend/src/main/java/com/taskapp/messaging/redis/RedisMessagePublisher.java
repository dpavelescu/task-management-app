package com.taskapp.messaging.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskapp.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Simple Redis message publisher for NotificationEvent objects.
 * Sends JSON directly to Redis pub/sub.
 */
@Service
@ConditionalOnProperty(name = "messaging.provider", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class RedisMessagePublisher implements MessagePublisher {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public void publishMessage(String topic, Object message) {
        publishMessage(topic, message, Map.of());
    }

    @Override
    public void publishMessage(String topic, Object message, Map<String, String> attributes) {        try {
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(topic, messageJson);
            log.debug("Published message to Redis topic '{}': {}", topic, messageJson);
                     
        } catch (Exception e) {
            log.error("Failed to publish message to Redis topic '{}': {}", topic, e.getMessage(), e);
            throw new RuntimeException("Message publishing failed", e);
        }
    }
      @Override
    public boolean isHealthy() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }
}
