package com.taskapp.service;

import com.taskapp.dto.NotificationEvent;
import com.taskapp.entity.Task;
import com.taskapp.entity.User;
import org.springframework.stereotype.Component;

@Component
public class NotificationFactory {

    public static class NotificationType {
        public static final String TASK_CREATED = "TASK_CREATED";
        public static final String TASK_ASSIGNED = "TASK_ASSIGNED";
        public static final String TASK_UPDATED = "TASK_UPDATED";
        public static final String TASK_REASSIGNED = "TASK_REASSIGNED";
        public static final String TASK_DELETED = "TASK_DELETED";
    }

    // Message templates for consistent formatting
    private enum MessageTemplate {
        TASK_CREATED("Task created: %s"),
        TASK_ASSIGNED("New task assigned: %s"),
        TASK_UPDATED("Task updated: %s"),
        TASK_STATUS_UPDATED("Task status updated: %s"),
        TASK_REASSIGNED("Task reassigned to you: %s"),
        TASK_DELETED("Task was deleted: %s");

        private final String template;

        MessageTemplate(String template) {
            this.template = template;
        }

        public String format(String taskTitle) {
            return String.format(template, taskTitle);
        }
    }    /**
     * Create task notification from Task entity
     */
    public NotificationEvent createTaskNotification(String type, Task task, User recipient) {
        MessageTemplate template = getMessageTemplate(type);
        String message = template.format(task.getTitle());
        
        return NotificationEvent.createTaskNotification(
                type,
                message,
                recipient.getUsername(),
                task.getId().toString(),
                task.getTitle(),
                task.getCreatedBy() != null ? task.getCreatedBy().getUsername() : null,
                task.getAssignedTo() != null ? task.getAssignedTo().getUsername() : null
        );
    }

    /**
     * Create task notification from Task entity with custom message template
     */
    public NotificationEvent createTaskNotification(String type, Task task, User recipient, MessageTemplate customTemplate) {
        String message = customTemplate.format(task.getTitle());
        
        return NotificationEvent.createTaskNotification(
                type,
                message,
                recipient.getUsername(),
                task.getId().toString(),
                task.getTitle(),
                task.getCreatedBy() != null ? task.getCreatedBy().getUsername() : null,
                task.getAssignedTo() != null ? task.getAssignedTo().getUsername() : null
        );
    }

    /**
     * Create task notification from task details (for cases where Task entity is not available)
     */
    public NotificationEvent createTaskNotification(String type, Long taskId, String taskTitle, User recipient) {
        MessageTemplate template = getMessageTemplate(type);
        String message = template.format(taskTitle);
        
        return NotificationEvent.createTaskNotification(
                type,
                message,
                recipient.getUsername(),
                taskId.toString(),
                taskTitle
        );
    }

    private MessageTemplate getMessageTemplate(String type) {
        return switch (type) {
            case NotificationType.TASK_CREATED -> MessageTemplate.TASK_CREATED;
            case NotificationType.TASK_ASSIGNED -> MessageTemplate.TASK_ASSIGNED;
            case NotificationType.TASK_UPDATED -> MessageTemplate.TASK_UPDATED;
            case NotificationType.TASK_REASSIGNED -> MessageTemplate.TASK_REASSIGNED;
            case NotificationType.TASK_DELETED -> MessageTemplate.TASK_DELETED;
            default -> MessageTemplate.TASK_UPDATED; // Safe fallback
        };
    }

    // Convenience methods for common use cases (backward compatibility)
    public NotificationEvent createTaskCreatedNotification(Task task, User recipient) {
        return createTaskNotification(NotificationType.TASK_CREATED, task, recipient);
    }

    public NotificationEvent createTaskAssignedNotification(Task task, User assignee) {
        return createTaskNotification(NotificationType.TASK_ASSIGNED, task, assignee);
    }

    public NotificationEvent createTaskUpdatedNotification(Task task, User recipient) {
        return createTaskNotification(NotificationType.TASK_UPDATED, task, recipient);
    }    public NotificationEvent createTaskStatusUpdatedNotification(Task task, User recipient) {
        return createTaskNotification(NotificationType.TASK_UPDATED, task, recipient, MessageTemplate.TASK_STATUS_UPDATED);
    }

    public NotificationEvent createTaskReassignedNotification(Task task, User newAssignee) {
        return createTaskNotification(NotificationType.TASK_REASSIGNED, task, newAssignee);
    }

    public NotificationEvent createTaskDeletedNotification(Task task, User recipient) {
        return createTaskNotification(NotificationType.TASK_DELETED, task, recipient);
    }

    public NotificationEvent createTaskDeletedNotification(Long taskId, String taskTitle, User recipient) {
        return createTaskNotification(NotificationType.TASK_DELETED, taskId, taskTitle, recipient);
    }
}
