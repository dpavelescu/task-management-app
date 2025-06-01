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
public class TaskService {    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;
    private final TaskPermissionHelper permissionHelper;
    private final DirectNotificationService directNotificationService;
    
    // Business event logger for audit trail
    private static final org.slf4j.Logger businessLog = org.slf4j.LoggerFactory.getLogger("business-events");    @Transactional
    public TaskResponse createTask(TaskRequest request, String creatorUsername) {
        log.debug("Creating task '{}' for user '{}'", request.getTitle(), creatorUsername);
        
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new UserNotFoundException("username", creatorUsername));
        
        // Determine assignee - assign to specified user or creator by default
        User assignee = (request.getAssignedTo() != null) 
            ? userRepository.findById(request.getAssignedTo())
                .orElseThrow(() -> new UserNotFoundException(request.getAssignedTo()))
            : creator;
        
        Task task = buildTaskFromRequest(request, creator, assignee);
        Task savedTask = taskRepository.save(task);
        
        // Flush to ensure immediate DB persistence before notifications
        taskRepository.flush();
        
        businessLog.info("TASK_CREATED: id={}, title='{}', creator={}, assignee={}",
                savedTask.getId(), savedTask.getTitle(), creator.getUsername(), assignee.getUsername());
        
        // Use direct notification service for immediate Redis publishing
        directNotificationService.publishTaskCreated(savedTask, creator, assignee);
        
        return taskMapper.toResponse(savedTask);
    }private Task buildTaskFromRequest(TaskRequest request, User creator, User assignee) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        // Status and priority will use default values from the entity
        if (request.getPriority() != null) {
            task.setPriority(TaskPriority.valueOf(request.getPriority()));
        }
        task.setCreatedBy(creator);
        task.setAssignedTo(assignee);        return task;
    }    public List<TaskResponse> getTasksForUser(String username) {
        log.debug("Retrieving tasks for user '{}'", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("username", username));
        
        List<Task> tasks = taskRepository.findByAssignedToOrCreatedBy(user, user);
        log.debug("Found {} tasks for user '{}'", tasks.size(), username);
        
        return tasks.stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest request, String username) {
        log.debug("Updating task {} by user '{}'", taskId, username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("username", username));
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        
        permissionHelper.validateUpdatePermission(task, user);
        
        User originalAssignee = task.getAssignedTo();
        boolean statusChanged = updateTaskFields(task, request);
        boolean assigneeChanged = handleAssigneeUpdate(task, request, user);
        
        Task updatedTask = taskRepository.save(task);
        
        // Flush to ensure immediate DB persistence before notifications
        taskRepository.flush();
        
        businessLog.info("TASK_UPDATED: id={}, title='{}', updatedBy={}, status={}", 
            updatedTask.getId(), updatedTask.getTitle(), username, updatedTask.getStatus());
        
        // Use direct notification service for immediate Redis publishing
        directNotificationService.publishTaskUpdated(updatedTask, user, originalAssignee, statusChanged, assigneeChanged);
        
        return taskMapper.toResponse(updatedTask);
    }private boolean updateTaskFields(Task task, TaskRequest request) {
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
        }        return false;
    }    @Transactional
    public void deleteTask(Long taskId, String username) {
        log.debug("Deleting task {} by user '{}'", taskId, username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("username", username));
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        
        permissionHelper.validateDeletePermission(task, user);
        
        // Store task info before deletion for notifications
        String taskTitle = task.getTitle();
        User creator = task.getCreatedBy();
        User assignee = task.getAssignedTo();
        
        // Delete the task from database FIRST and flush to ensure it's committed
        taskRepository.delete(task);
        taskRepository.flush(); // Ensure the deletion is immediately flushed to the database
        
        log.debug("Task {} deleted successfully", taskId);
        
        businessLog.info("TASK_DELETED: id={}, title='{}', deletedBy={}", 
            taskId, taskTitle, username);
        
        // Use direct notification service for immediate Redis publishing after DB commit
        directNotificationService.publishTaskDeleted(taskId, taskTitle, creator, assignee);
    }
}
