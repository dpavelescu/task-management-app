package com.taskapp.messaging;

/**
 * Simple message consumer interface for Redis subscriptions.
 */
public interface MessageConsumer {
    
    /**
     * Subscribe to a topic with a message handler
     */
    void subscribe(String topic, MessageHandler handler);
    
    /**
     * Unsubscribe from a topic
     */
    void unsubscribe(String topic);
    
    /**
     * Start consuming messages
     */
    void startConsuming();
    
    /**
     * Stop consuming messages
     */
    void stopConsuming();
    
    /**
     * Check if the consumer is healthy
     */
    boolean isHealthy();
}
