package com.taskapp.mapper;

import com.taskapp.dto.TaskRequest;
import com.taskapp.dto.TaskResponse;
import com.taskapp.entity.Task;
import com.taskapp.entity.User;
import com.taskapp.enums.TaskPriority;
import com.taskapp.enums.TaskStatus;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {    public TaskResponse toResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus().getValue());
        response.setPriority(task.getPriority().getValue());
        response.setCreatedById(task.getCreatedBy().getId());
        response.setCreatedByUsername(task.getCreatedBy().getUsername());
        response.setAssignedToId(task.getAssignedTo().getId());
        response.setAssignedToUsername(task.getAssignedTo().getUsername());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }    public Task toEntity(TaskRequest request, User creator, User assignee) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        // Status will use default value (PENDING) from entity
        if (request.getPriority() != null) {
            task.setPriority(TaskPriority.fromValue(request.getPriority()));
        }
        // Priority will use default value (MEDIUM) from entity if not specified
        task.setCreatedBy(creator);
        task.setAssignedTo(assignee);
        return task;
    }    public void updateFromRequest(Task task, TaskRequest request) {
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(TaskStatus.fromValue(request.getStatus()));
        }
        if (request.getPriority() != null) {
            task.setPriority(TaskPriority.fromValue(request.getPriority()));
        }
    }
}
