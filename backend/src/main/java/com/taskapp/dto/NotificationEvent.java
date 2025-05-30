package com.taskapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationEvent {
    @Builder.Default
    private String id = generateId();
    
    private String type;
    private String message;
    private String taskId;
    private String taskTitle;
    private String username;
    
    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();
    
    private String creatorUsername;
    private String assignedUsername;

    private static String generateId() {
        return System.currentTimeMillis() + "-" + Math.random();
    }
}
