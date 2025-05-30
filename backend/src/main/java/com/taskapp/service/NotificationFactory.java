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

    public NotificationEvent createTaskCreatedNotification(Task task, User recipient) {
        return NotificationEvent.builder()
                .type(NotificationType.TASK_CREATED)
                .message("Task created: " + task.getTitle())
                .taskId(task.getId().toString())
                .taskTitle(task.getTitle())
                .username(recipient.getUsername())
                .creatorUsername(task.getCreatedBy().getUsername())
                .assignedUsername(task.getAssignedTo().getUsername())
                .build();
    }

    public NotificationEvent createTaskAssignedNotification(Task task, User assignee) {
        return NotificationEvent.builder()
                .type(NotificationType.TASK_ASSIGNED)
                .message("New task assigned: " + task.getTitle())
                .taskId(task.getId().toString())
                .taskTitle(task.getTitle())
                .username(assignee.getUsername())
                .creatorUsername(task.getCreatedBy().getUsername())
                .assignedUsername(assignee.getUsername())
                .build();
    }

    public NotificationEvent createTaskUpdatedNotification(Task task, User recipient) {
        return NotificationEvent.builder()
                .type(NotificationType.TASK_UPDATED)
                .message("Task updated: " + task.getTitle())
                .taskId(task.getId().toString())
                .taskTitle(task.getTitle())
                .username(recipient.getUsername())
                .build();
    }

    public NotificationEvent createTaskStatusUpdatedNotification(Task task, User recipient) {
        return NotificationEvent.builder()
                .type(NotificationType.TASK_UPDATED)
                .message("Task status updated: " + task.getTitle())
                .taskId(task.getId().toString())
                .taskTitle(task.getTitle())
                .username(recipient.getUsername())
                .build();
    }

    public NotificationEvent createTaskReassignedNotification(Task task, User newAssignee) {
        return NotificationEvent.builder()
                .type(NotificationType.TASK_REASSIGNED)
                .message("Task reassigned to you: " + task.getTitle())
                .taskId(task.getId().toString())
                .taskTitle(task.getTitle())
                .username(newAssignee.getUsername())
                .build();
    }    public NotificationEvent createTaskDeletedNotification(Task task, User recipient) {
        return NotificationEvent.builder()
                .type(NotificationType.TASK_DELETED)
                .message("Task was deleted: " + task.getTitle())
                .taskId(task.getId().toString())
                .taskTitle(task.getTitle())
                .username(recipient.getUsername())
                .build();
    }

    public NotificationEvent createTaskDeletedNotification(Long taskId, String taskTitle, User recipient) {
        return NotificationEvent.builder()
                .type(NotificationType.TASK_DELETED)
                .message("Task was deleted: " + taskTitle)
                .taskId(taskId.toString())
                .taskTitle(taskTitle)
                .username(recipient.getUsername())
                .build();
    }
}
