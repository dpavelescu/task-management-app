package com.taskapp.messaging.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskapp.messaging.config.MessagingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for pub/sub messaging.
 */
@Configuration
@Slf4j
public class RedisMessagingConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                       ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        log.info("Configured Redis template for pub/sub messaging");
        return template;
    }    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisMessageConsumer messageConsumer,
            MessagingProperties messagingProperties) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // Configure container for optimal pub/sub performance with better error handling
        container.setTaskExecutor(null); // Use default thread pool
        container.setSubscriptionExecutor(null); // Use default subscription executor
        
        // Configure recovery and error handling
        container.setRecoveryInterval(5000L); // 5 seconds recovery interval
        container.setMaxSubscriptionRegistrationWaitingTime(10000L); // 10 seconds max wait
        
        // Register the message consumer for user notifications topic
        String topic = messagingProperties.getTopics().getUserNotifications();
        container.addMessageListener(messageConsumer, new ChannelTopic(topic));
        
        log.info("Configured Redis message listener container with recovery settings and topic: {}", topic);
        return container;
    }
}
