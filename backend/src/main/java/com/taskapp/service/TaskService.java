package com.taskapp.service;

import com.taskapp.dto.TaskRequest;
import com.taskapp.dto.TaskResponse;
import com.taskapp.entity.Task;
import com.taskapp.entity.User;
import com.taskapp.enums.TaskStatus;
import com.taskapp.enums.TaskPriority;
import com.taskapp.exception.TaskNotFoundException;
import com.taskapp.exception.UserNotFoundException;
import com.taskapp.repository.TaskRepository;
import com.taskapp.repository.UserRepository;
import com.taskapp.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final TaskMapper taskMapper;
    private final TaskPermissionHelper permissionHelper;
    private final NotificationFactory notificationFactory;
    
    // Business event logger for audit trail
    private static final org.slf4j.Logger businessLog = org.slf4j.LoggerFactory.getLogger("business-events");    @Transactional
    public TaskResponse createTask(TaskRequest request, String creatorUsername) {
        log.debug("Creating task '{}' for user '{}'", request.getTitle(), creatorUsername);
        
        User creator = findUserByUsername(creatorUsername);
        User assignee = determineTaskAssignee(request, creator);
        
        Task task = buildTaskFromRequest(request, creator, assignee);
        Task savedTask = taskRepository.save(task);
        
        logTaskCreation(savedTask, creator, assignee);
        sendTaskCreationNotifications(savedTask, creator, assignee);
        
        return taskMapper.toResponse(savedTask);
    }
    
    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("username", username));
    }
    
    private User determineTaskAssignee(TaskRequest request, User creator) {
        if (request.getAssignedTo() != null) {
            return userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new UserNotFoundException(request.getAssignedTo()));
        }
        return creator; // Assign to creator by default
    }    private Task buildTaskFromRequest(TaskRequest request, User creator, User assignee) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        // Status and priority will use default values from the entity
        if (request.getPriority() != null) {
            task.setPriority(TaskPriority.valueOf(request.getPriority()));
        }
        task.setCreatedBy(creator);
        task.setAssignedTo(assignee);
        return task;
    }
    
    private void logTaskCreation(Task task, User creator, User assignee) {
        businessLog.info("TASK_CREATED: id={}, title='{}', creator={}, assignee={}",
                task.getId(), task.getTitle(), creator.getUsername(), assignee.getUsername());
    }
    
    private void sendTaskCreationNotifications(Task task, User creator, User assignee) {
        // Always send a TASK_CREATED notification to the creator
        var creatorNotification = notificationFactory.createTaskCreatedNotification(task, creator);
        log.debug("Sending TASK_CREATED notification to creator: {}", creator.getUsername());
        notificationService.sendNotification(creatorNotification);
        
        // If assigned to someone else, also send a TASK_ASSIGNED notification to the assignee
        if (!assignee.equals(creator)) {
            var assigneeNotification = notificationFactory.createTaskAssignedNotification(task, assignee);
            log.debug("Sending TASK_ASSIGNED notification to assignee: {}", assignee.getUsername());
            notificationService.sendNotification(assigneeNotification);
        }
    }    public List<TaskResponse> getTasksForUser(String username) {
        log.debug("Retrieving tasks for user '{}'", username);
        
        User user = findUserByUsername(username);
        List<Task> tasks = taskRepository.findByAssignedToOrCreatedBy(user, user);
        log.debug("Found {} tasks for user '{}'", tasks.size(), username);
        
        return tasks.stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest request, String username) {
        log.debug("Updating task {} by user '{}'", taskId, username);
        
        User user = findUserByUsername(username);
        Task task = findTaskById(taskId);
        
        permissionHelper.validateUpdatePermission(task, user);
        
        User originalAssignee = task.getAssignedTo();
        boolean statusChanged = updateTaskFields(task, request);
        boolean assigneeChanged = handleAssigneeUpdate(task, request, user);
        
        Task updatedTask = taskRepository.save(task);
        logTaskUpdate(updatedTask, username);
        sendTaskUpdateNotifications(updatedTask, user, originalAssignee, statusChanged, assigneeChanged);
        
        return taskMapper.toResponse(updatedTask);
    }
    
    private Task findTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }    private boolean updateTaskFields(Task task, TaskRequest request) {
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        
        boolean statusChanged = false;
        if (request.getStatus() != null) {
            TaskStatus newStatus = TaskStatus.valueOf(request.getStatus());
            statusChanged = !newStatus.equals(task.getStatus());
            task.setStatus(newStatus);
        }
        if (request.getPriority() != null) {
            task.setPriority(TaskPriority.valueOf(request.getPriority()));
        }
        return statusChanged;
    }
    
    private boolean handleAssigneeUpdate(Task task, TaskRequest request, User user) {
        if (request.getAssignedTo() != null && task.getCreatedBy().equals(user)) {
            User newAssignee = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new UserNotFoundException(request.getAssignedTo()));
            
            boolean assigneeChanged = !task.getAssignedTo().equals(newAssignee);
            task.setAssignedTo(newAssignee);
            return assigneeChanged;
        }
        return false;
    }
    
    private void logTaskUpdate(Task task, String username) {
        businessLog.info("TASK_UPDATED: id={}, title='{}', updatedBy={}, status={}", 
            task.getId(), task.getTitle(), username, task.getStatus());
    }
    
    private void sendTaskUpdateNotifications(Task task, User user, User originalAssignee, boolean statusChanged, boolean assigneeChanged) {
        // Always notify the creator if they're not the one making the update
        if (!task.getCreatedBy().equals(user)) {
            var notification = notificationFactory.createTaskUpdatedNotification(task, task.getCreatedBy());
            log.debug("Sending TASK_UPDATED notification to creator: {}", task.getCreatedBy().getUsername());
            notificationService.sendNotification(notification);
        }
        
        // Send notification to original assignee when task status changes (if not the one updating it)
        if (statusChanged && !originalAssignee.equals(user)) {
            var notification = notificationFactory.createTaskUpdatedNotification(task, originalAssignee);
            log.debug("Sending TASK_UPDATED notification to assignee: {}", originalAssignee.getUsername());
            notificationService.sendNotification(notification);
        }
        
        // If assignee changed, notify the new assignee (if different from user)
        if (assigneeChanged && !task.getAssignedTo().equals(user)) {
            var notification = notificationFactory.createTaskReassignedNotification(task, task.getAssignedTo());
            log.debug("Sending TASK_REASSIGNED notification to new assignee: {}", task.getAssignedTo().getUsername());
            notificationService.sendNotification(notification);
        }    }

    @Transactional
    public void deleteTask(Long taskId, String username) {
                log.debug("Deleting task {} by user '{}'", taskId, username);
        
        User user = findUserByUsername(username);
        Task task = findTaskById(taskId);
        
        permissionHelper.validateDeletePermission(task, user);
        
        // Store task info before deletion for notifications
        String taskTitle = task.getTitle();
        User creator = task.getCreatedBy();
        User assignee = task.getAssignedTo();
        
        // Delete the task from database FIRST
        taskRepository.delete(task);
        log.debug("Task {} deleted successfully", taskId);
        logTaskDeletion(taskId, taskTitle, username);
        
        // Send SSE notifications AFTER deletion is complete
        sendTaskDeletionNotifications(taskId, taskTitle, creator, assignee);
    }
    
    private void logTaskDeletion(Long taskId, String taskTitle, String username) {
        businessLog.info("TASK_DELETED: id={}, title='{}', deletedBy={}", 
            taskId, taskTitle, username);
    }
    
    private void sendTaskDeletionNotifications(Long taskId, String taskTitle, User creator, User assignee) {
        // Always notify the creator
        var creatorNotification = notificationFactory.createTaskDeletedNotification(taskId, taskTitle, creator);
        log.debug("Sending TASK_DELETED notification to creator: {}", creator.getUsername());
        notificationService.sendNotification(creatorNotification);
        
        // Notify assignee if different from creator
        if (!assignee.equals(creator)) {
            var assigneeNotification = notificationFactory.createTaskDeletedNotification(taskId, taskTitle, assignee);
            log.debug("Sending TASK_DELETED notification to assignee: {}", assignee.getUsername());
            notificationService.sendNotification(assigneeNotification);        }
    }
}
