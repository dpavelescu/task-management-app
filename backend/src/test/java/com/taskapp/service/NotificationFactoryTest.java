package com.taskapp.service;

import com.taskapp.dto.NotificationEvent;
import com.taskapp.entity.Task;
import com.taskapp.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for NotificationFactory to ensure it correctly creates NotificationEvent objects
 * using the new static factory methods.
 */
class NotificationFactoryTest {
    
    private NotificationFactory notificationFactory;
    private User testUser;
    private User creatorUser;
    private User assigneeUser;
    private Task testTask;
    
    @BeforeEach
    void setUp() {
        notificationFactory = new NotificationFactory();
        
        // Create test users
        testUser = mock(User.class);
        when(testUser.getUsername()).thenReturn("testuser");
        
        creatorUser = mock(User.class);
        when(creatorUser.getUsername()).thenReturn("creator");
        
        assigneeUser = mock(User.class);
        when(assigneeUser.getUsername()).thenReturn("assignee");
        
        // Create test task
        testTask = mock(Task.class);
        when(testTask.getId()).thenReturn(123L);
        when(testTask.getTitle()).thenReturn("Test Task");
        when(testTask.getCreatedBy()).thenReturn(creatorUser);
        when(testTask.getAssignedTo()).thenReturn(assigneeUser);
    }
    
    @Test
    @DisplayName("Should create task created notification with all required fields")
    void testCreateTaskCreatedNotification() {
        // When: Create task created notification
        NotificationEvent event = notificationFactory.createTaskCreatedNotification(testTask, testUser);
        
        // Then: Event should have correct values
        assertAll(
            () -> assertNotNull(event.getId()),
            () -> assertEquals(NotificationFactory.NotificationType.TASK_CREATED, event.getType()),
            () -> assertEquals("Task created: Test Task", event.getMessage()),
            () -> assertEquals("testuser", event.getUsername()),
            () -> assertEquals("123", event.getTaskId()),
            () -> assertEquals("Test Task", event.getTaskTitle()),
            () -> assertEquals("creator", event.getCreatorUsername()),
            () -> assertEquals("assignee", event.getAssignedUsername()),
            () -> assertNotNull(event.getTimestamp())
        );
    }
    
    @Test
    @DisplayName("Should create task assigned notification with all required fields")
    void testCreateTaskAssignedNotification() {
        // When: Create task assigned notification
        NotificationEvent event = notificationFactory.createTaskAssignedNotification(testTask, assigneeUser);
        
        // Then: Event should have correct values
        assertAll(
            () -> assertNotNull(event.getId()),
            () -> assertEquals(NotificationFactory.NotificationType.TASK_ASSIGNED, event.getType()),
            () -> assertEquals("New task assigned: Test Task", event.getMessage()),
            () -> assertEquals("assignee", event.getUsername()),
            () -> assertEquals("123", event.getTaskId()),
            () -> assertEquals("Test Task", event.getTaskTitle()),
            () -> assertEquals("creator", event.getCreatorUsername()),
            () -> assertEquals("assignee", event.getAssignedUsername()),
            () -> assertNotNull(event.getTimestamp())
        );
    }
    
    @Test
    @DisplayName("Should create task updated notification with required fields")
    void testCreateTaskUpdatedNotification() {
        // When: Create task updated notification
        NotificationEvent event = notificationFactory.createTaskUpdatedNotification(testTask, testUser);
        
        // Then: Event should have correct values
        assertAll(
            () -> assertNotNull(event.getId()),
            () -> assertEquals(NotificationFactory.NotificationType.TASK_UPDATED, event.getType()),
            () -> assertEquals("Task updated: Test Task", event.getMessage()),
            () -> assertEquals("testuser", event.getUsername()),
            () -> assertEquals("123", event.getTaskId()),
            () -> assertEquals("Test Task", event.getTaskTitle()),
            () -> assertNotNull(event.getTimestamp())
        );
    }
    
    @Test
    @DisplayName("Should create task status updated notification with required fields")
    void testCreateTaskStatusUpdatedNotification() {
        // When: Create task status updated notification
        NotificationEvent event = notificationFactory.createTaskStatusUpdatedNotification(testTask, testUser);
        
        // Then: Event should have correct values
        assertAll(
            () -> assertNotNull(event.getId()),
            () -> assertEquals(NotificationFactory.NotificationType.TASK_UPDATED, event.getType()),
            () -> assertEquals("Task status updated: Test Task", event.getMessage()),
            () -> assertEquals("testuser", event.getUsername()),
            () -> assertEquals("123", event.getTaskId()),
            () -> assertEquals("Test Task", event.getTaskTitle()),
            () -> assertNotNull(event.getTimestamp())
        );
    }
    
    @Test
    @DisplayName("Should create task reassigned notification with required fields")
    void testCreateTaskReassignedNotification() {
        // When: Create task reassigned notification
        NotificationEvent event = notificationFactory.createTaskReassignedNotification(testTask, assigneeUser);
        
        // Then: Event should have correct values
        assertAll(
            () -> assertNotNull(event.getId()),
            () -> assertEquals(NotificationFactory.NotificationType.TASK_REASSIGNED, event.getType()),
            () -> assertEquals("Task reassigned to you: Test Task", event.getMessage()),
            () -> assertEquals("assignee", event.getUsername()),
            () -> assertEquals("123", event.getTaskId()),
            () -> assertEquals("Test Task", event.getTaskTitle()),
            () -> assertNotNull(event.getTimestamp())
        );
    }
    
    @Test
    @DisplayName("Should create task deleted notification with task object")
    void testCreateTaskDeletedNotificationWithTask() {
        // When: Create task deleted notification with task object
        NotificationEvent event = notificationFactory.createTaskDeletedNotification(testTask, testUser);
        
        // Then: Event should have correct values
        assertAll(
            () -> assertNotNull(event.getId()),
            () -> assertEquals(NotificationFactory.NotificationType.TASK_DELETED, event.getType()),
            () -> assertEquals("Task was deleted: Test Task", event.getMessage()),
            () -> assertEquals("testuser", event.getUsername()),
            () -> assertEquals("123", event.getTaskId()),
            () -> assertEquals("Test Task", event.getTaskTitle()),
            () -> assertNotNull(event.getTimestamp())
        );
    }
    
    @Test
    @DisplayName("Should create task deleted notification with task ID and title")
    void testCreateTaskDeletedNotificationWithIdAndTitle() {
        // When: Create task deleted notification with ID and title
        NotificationEvent event = notificationFactory.createTaskDeletedNotification(456L, "Deleted Task", testUser);
        
        // Then: Event should have correct values
        assertAll(
            () -> assertNotNull(event.getId()),
            () -> assertEquals(NotificationFactory.NotificationType.TASK_DELETED, event.getType()),
            () -> assertEquals("Task was deleted: Deleted Task", event.getMessage()),
            () -> assertEquals("testuser", event.getUsername()),
            () -> assertEquals("456", event.getTaskId()),
            () -> assertEquals("Deleted Task", event.getTaskTitle()),
            () -> assertNotNull(event.getTimestamp())
        );
    }
    
    @Test
    @DisplayName("Should generate unique IDs for different notifications")
    void testUniqueNotificationIds() {
        // When: Create multiple notifications
        NotificationEvent event1 = notificationFactory.createTaskCreatedNotification(testTask, testUser);
        NotificationEvent event2 = notificationFactory.createTaskAssignedNotification(testTask, assigneeUser);
        NotificationEvent event3 = notificationFactory.createTaskUpdatedNotification(testTask, testUser);
        
        // Then: All should have unique IDs
        assertAll(
            () -> assertNotNull(event1.getId()),
            () -> assertNotNull(event2.getId()),
            () -> assertNotNull(event3.getId()),
            () -> assertNotEquals(event1.getId(), event2.getId()),
            () -> assertNotEquals(event2.getId(), event3.getId()),
            () -> assertNotEquals(event1.getId(), event3.getId())
        );
    }
    
    @Test
    @DisplayName("Should handle task with null assigned user gracefully")
    void testTaskWithNullAssignedUser() {
        // Given: Task with null assigned user
        when(testTask.getAssignedTo()).thenReturn(null);
        
        // When: Create notification
        // Then: Should not throw exception
        assertDoesNotThrow(() -> {
            NotificationEvent event = notificationFactory.createTaskCreatedNotification(testTask, testUser);
            assertNotNull(event);
            assertNull(event.getAssignedUsername());
        });
    }
    
    @Test
    @DisplayName("Should handle task with null creator gracefully")
    void testTaskWithNullCreator() {
        // Given: Task with null creator
        when(testTask.getCreatedBy()).thenReturn(null);
        
        // When: Create notification
        // Then: Should not throw exception
        assertDoesNotThrow(() -> {
            NotificationEvent event = notificationFactory.createTaskCreatedNotification(testTask, testUser);
            assertNotNull(event);
            assertNull(event.getCreatorUsername());
        });
    }
}
