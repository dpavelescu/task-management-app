package com.taskapp.messaging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Redis messaging system.
 */
@Configuration
@ConfigurationProperties(prefix = "messaging")
@Data
public class MessagingProperties {
    
    /**
     * Messaging provider (currently only Redis is supported)
     */
    private String provider = "redis";
    
    /**
     * Unique identifier for this pod/instance
     */
    private String podId = "default-pod";
    
    /**
     * Topic names for different message types
     */
    private Topics topics = new Topics();
    
    @Data
    public static class Topics {
        private String userNotifications = "user-notifications";
        private String systemEvents = "system-events";
        private String taskLifecycle = "task-lifecycle";
    }
}
