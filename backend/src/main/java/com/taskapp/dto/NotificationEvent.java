package com.taskapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Simple notification event for Redis pub/sub messaging.
 * Simplified for easy serialization/deserialization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationEvent {
    private String id;
    private String type;
    private String message;
    private String taskId;    private String taskTitle;
    private String username;    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private String creatorUsername;
    private String assignedUsername;
      /**
     * Create a new notification with basic fields
     */
    public static NotificationEvent create(String type, String message, String username) {
        NotificationEvent event = new NotificationEvent();
        event.setId(generateId());
        event.setType(type);
        event.setMessage(message);
        event.setUsername(username);
        event.setTimestamp(LocalDateTime.now().withNano(0)); // Truncate to seconds for consistent serialization
        return event;
    }
      /**
     * Create a new task-related notification
     */
    public static NotificationEvent createTaskNotification(String type, String message, 
                                                          String username, String taskId, String taskTitle) {
        NotificationEvent event = create(type, message, username);
        event.setTaskId(taskId);
        event.setTaskTitle(taskTitle);
        return event;
    }
    
    /**
     * Create a comprehensive task notification with all fields
     */
    public static NotificationEvent createTaskNotification(String type, String message, 
                                                          String username, String taskId, String taskTitle,
                                                          String creatorUsername, String assignedUsername) {
        NotificationEvent event = createTaskNotification(type, message, username, taskId, taskTitle);
        event.setCreatorUsername(creatorUsername);
        event.setAssignedUsername(assignedUsername);
        return event;
    }
    
    /**
     * Generate a unique event ID
     */
    public static String generateId() {
        return System.currentTimeMillis() + "-" + Math.random();
    }
}
