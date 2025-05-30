package com.taskapp.dto;

import lombok.Data;
import java.time.ZonedDateTime;

@Data
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private Long createdById;
    private String createdByUsername;
    private Long assignedToId;
    private String assignedToUsername;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
