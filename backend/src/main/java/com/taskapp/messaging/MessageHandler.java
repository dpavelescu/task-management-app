package com.taskapp.messaging;

import java.util.Map;

/**
 * Handler interface for processing incoming Redis messages.
 */
@FunctionalInterface
public interface MessageHandler {
    
    /**
     * Handle an incoming message
     * @param topic The topic/queue the message came from
     * @param message The message payload (typically a JSON string or deserialized object)
     * @param attributes Message attributes/headers for routing and metadata
     * @throws Exception if message processing fails (will trigger retry/DLQ logic)
     */
    void handleMessage(String topic, Object message, Map<String, String> attributes) throws Exception;
}
