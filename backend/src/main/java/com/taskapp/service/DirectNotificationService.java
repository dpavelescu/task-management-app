package com.taskapp.service;

import com.taskapp.entity.Task;
import com.taskapp.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Direct notification service that publishes notifications immediately after DB operations.
 * This service bypasses Spring events and publishes directly to Redis for consistency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DirectNotificationService {
    
    private final NotificationService notificationService;
    private final NotificationFactory notificationFactory;
    
    /**
     * Publish task created notifications directly to Redis
     */
    public void publishTaskCreated(Task task, User creator, User assignee) {
        log.debug("Publishing TASK_CREATED notifications for task {}", task.getId());
        
        // Always notify creator
        var creatorNotification = notificationFactory.createTaskCreatedNotification(task, creator);
        log.debug("Sending TASK_CREATED notification to creator: {}", creator.getUsername());
        notificationService.sendNotification(creatorNotification);
        
        // Notify assignee if different from creator
        if (!assignee.equals(creator)) {
            var assigneeNotification = notificationFactory.createTaskAssignedNotification(task, assignee);
            log.debug("Sending TASK_ASSIGNED notification to assignee: {}", assignee.getUsername());
            notificationService.sendNotification(assigneeNotification);
        }
    }
    
    /**
     * Publish task deleted notifications directly to Redis
     */
    public void publishTaskDeleted(Long taskId, String taskTitle, User creator, User assignee) {
        log.debug("Publishing TASK_DELETED notifications for task {}", taskId);
        
        // Always notify creator
        var creatorNotification = notificationFactory.createTaskDeletedNotification(taskId, taskTitle, creator);
        log.debug("Sending TASK_DELETED notification to creator: {}", creator.getUsername());
        notificationService.sendNotification(creatorNotification);
        
        // Notify assignee if different from creator
        if (!assignee.equals(creator)) {
            var assigneeNotification = notificationFactory.createTaskDeletedNotification(taskId, taskTitle, assignee);
            log.debug("Sending TASK_DELETED notification to assignee: {}", assignee.getUsername());
            notificationService.sendNotification(assigneeNotification);
        }
    }
    
    /**
     * Publish task updated notifications directly to Redis
     */
    public void publishTaskUpdated(Task task, User updatedBy, User originalAssignee, boolean statusChanged, boolean assigneeChanged) {
        log.debug("Publishing TASK_UPDATED notifications for task {}", task.getId());
        
        // Always notify the creator if they're not the one making the update
        if (!task.getCreatedBy().equals(updatedBy)) {
            var notification = notificationFactory.createTaskUpdatedNotification(task, task.getCreatedBy());
            log.debug("Sending TASK_UPDATED notification to creator: {}", task.getCreatedBy().getUsername());
            notificationService.sendNotification(notification);
        }
        
        // Send notification to original assignee when task status changes (if not the one updating it)
        if (statusChanged && !originalAssignee.equals(updatedBy)) {
            var notification = notificationFactory.createTaskUpdatedNotification(task, originalAssignee);
            log.debug("Sending TASK_UPDATED notification to assignee: {}", originalAssignee.getUsername());
            notificationService.sendNotification(notification);
        }
        
        // If assignee changed, notify the new assignee (if different from user)
        if (assigneeChanged && !task.getAssignedTo().equals(updatedBy)) {
            var notification = notificationFactory.createTaskReassignedNotification(task, task.getAssignedTo());
            log.debug("Sending TASK_REASSIGNED notification to new assignee: {}", task.getAssignedTo().getUsername());
            notificationService.sendNotification(notification);
        }
    }
}
