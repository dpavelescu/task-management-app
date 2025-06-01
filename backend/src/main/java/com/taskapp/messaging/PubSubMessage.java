package com.taskapp.messaging;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Message wrapper for Redis pub/sub messaging.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = PubSubMessage.PubSubMessageBuilder.class)
public class PubSubMessage {
    
    /**
     * Unique message identifier
     */
    @JsonProperty("messageId")
    private String messageId;
    
    /**
     * Topic name for Redis channel
     */
    @JsonProperty("topicArn")
    private String topicArn;
    
    /**
     * Message subject/type - used for message filtering
     */
    @JsonProperty("subject")
    private String subject;
    
    /**
     * JSON message payload
     */
    @JsonProperty("message")
    private String message;
    
    /**
     * Message attributes for routing and metadata
     */
    @JsonProperty("messageAttributes")
    private Map<String, String> messageAttributes;
    
    /**
     * Message timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    /**
     * Generate a unique message ID
     */
    public static String generateMessageId() {
        return System.currentTimeMillis() + "-" + Math.random();
    }
    
    /**
     * Jackson builder configuration for proper deserialization
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static class PubSubMessageBuilder {
        // Lombok will generate the builder methods
        
        // Custom method to set default timestamp if not provided
        public PubSubMessageBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
            return this;
        }
        
        // Custom method to set default messageId if not provided
        public PubSubMessageBuilder messageId(String messageId) {
            this.messageId = messageId != null ? messageId : generateMessageId();
            return this;
        }
    }
}
