package com.taskapp.messaging;

import java.util.Map;

/**
 * Simple message publishing interface for Redis pub/sub.
 */
public interface MessagePublisher {
    
    /**
     * Publish a message to a topic
     */
    void publishMessage(String topic, Object message);
    
    /**
     * Publish a message to a topic with attributes
     */
    void publishMessage(String topic, Object message, Map<String, String> attributes);
    
    /**
     * Check if the publisher is healthy
     */
    boolean isHealthy();
}
