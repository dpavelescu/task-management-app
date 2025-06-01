package com.taskapp.messaging.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskapp.dto.NotificationEvent;
import com.taskapp.entity.Task;
import com.taskapp.entity.User;
import com.taskapp.service.NotificationFactory;
import com.taskapp.service.SSEConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * End-to-end test for the complete Redis messaging flow.
 * Tests NotificationFactory -> RedisMessagePublisher -> RedisMessageConsumer.
 */
@ExtendWith(MockitoExtension.class)
class EndToEndRedisMessagingTest {    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private SSEConnectionManager sseConnectionManager;
    
    private NotificationFactory notificationFactory;
    private RedisMessagePublisher publisher;
    private RedisMessageConsumer consumer;
    private ObjectMapper objectMapper;
      @BeforeEach
    void setUp() {
        // Setup ObjectMapper with time module
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
          
        // Create real instances with mocked dependencies
        notificationFactory = new NotificationFactory();
        publisher = new RedisMessagePublisher(redisTemplate, objectMapper);
        consumer = new RedisMessageConsumer(sseConnectionManager, objectMapper);
        
        // Set up lenient mocking to avoid unnecessary stubbing exceptions
        lenient().doNothing().when(sseConnectionManager).sendToUserLocal(anyString(), any(NotificationEvent.class));
    }
      @Test
    @DisplayName("Should complete end-to-end flow: NotificationFactory -> Publisher -> Consumer")
    void testCompleteEndToEndFlow() throws Exception {
        // Given: Test entities
        User creator = mock(User.class);
        when(creator.getUsername()).thenReturn("creator123");
        
        User assignee = mock(User.class);
        when(assignee.getUsername()).thenReturn("assignee123");
        
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(456L);
        when(task.getTitle()).thenReturn("Integration Test Task");
        when(task.getCreatedBy()).thenReturn(creator);
        when(task.getAssignedTo()).thenReturn(assignee);
        
        String topic = "user-notifications";
        AtomicReference<String> capturedJson = new AtomicReference<>();
        
        // Capture the JSON that would be sent to Redis
        doAnswer(invocation -> {
            String json = invocation.getArgument(1);
            capturedJson.set(json);
            return null;
        }).when(redisTemplate).convertAndSend(eq(topic), anyString());
        
        // When: Publish message and simulate Redis delivering it to consumer
        NotificationEvent originalEvent = notificationFactory.createTaskCreatedNotification(task, assignee);
        publisher.publishMessage(topic, originalEvent);
        
        // Simulate the message delivery directly
        String jsonMessage = objectMapper.writeValueAsString(originalEvent);
        Message mockMessage = mock(Message.class);
        when(mockMessage.getBody()).thenReturn(jsonMessage.getBytes());
        when(mockMessage.getChannel()).thenReturn(topic.getBytes());
        
        // Call the consumer's onMessage method directly - this should not throw any exceptions
        assertDoesNotThrow(() -> consumer.onMessage(mockMessage, null));
        
        // Verify the JSON was properly formed and sent to Redis
        String json = capturedJson.get();
        assertNotNull(json, "No JSON was captured from Redis publish");
        
        // Verify the JSON contains expected content
        assertAll(
            () -> assertTrue(json.contains("\"type\":\"TASK_CREATED\"")),
            () -> assertTrue(json.contains("\"message\":\"Task created: Integration Test Task\"")),
            () -> assertTrue(json.contains("\"username\":\"assignee123\"")),
            () -> assertTrue(json.contains("\"taskId\":\"456\"")),
            () -> assertTrue(json.contains("\"taskTitle\":\"Integration Test Task\"")),
            () -> assertTrue(json.contains("\"creatorUsername\":\"creator123\"")),
            () -> assertTrue(json.contains("\"assignedUsername\":\"assignee123\""))
        );
        
        // Verify the message can be deserialized correctly
        NotificationEvent deserializedEvent = objectMapper.readValue(json, NotificationEvent.class);
        assertAll(
            () -> assertEquals(originalEvent.getId(), deserializedEvent.getId()),
            () -> assertEquals(NotificationFactory.NotificationType.TASK_CREATED, deserializedEvent.getType()),
            () -> assertEquals("Task created: Integration Test Task", deserializedEvent.getMessage()),
            () -> assertEquals("assignee123", deserializedEvent.getUsername()),
            () -> assertEquals("456", deserializedEvent.getTaskId()),
            () -> assertEquals("Integration Test Task", deserializedEvent.getTaskTitle()),
            () -> assertEquals("creator123", deserializedEvent.getCreatorUsername()),
            () -> assertEquals("assignee123", deserializedEvent.getAssignedUsername()),
            () -> assertEquals(originalEvent.getTimestamp(), deserializedEvent.getTimestamp())
        );
    }
    
    @Test
    @DisplayName("Should handle all notification types end-to-end")
    void testAllNotificationTypesEndToEnd() throws Exception {
        // Given: Test entities
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("testuser");
        
        User creator = mock(User.class);
        when(creator.getUsername()).thenReturn("creator");
        
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(789L);
        when(task.getTitle()).thenReturn("Multi-Type Test Task");
        when(task.getCreatedBy()).thenReturn(creator);        when(task.getAssignedTo()).thenReturn(user);
        
        // Test each notification type
        NotificationEvent[] testEvents = {
            notificationFactory.createTaskCreatedNotification(task, user),
            notificationFactory.createTaskAssignedNotification(task, user),
            notificationFactory.createTaskUpdatedNotification(task, user),
            notificationFactory.createTaskStatusUpdatedNotification(task, user),
            notificationFactory.createTaskReassignedNotification(task, user),
            notificationFactory.createTaskDeletedNotification(task, user),
            notificationFactory.createTaskDeletedNotification(999L, "Deleted Task", user)
        };
        
        for (NotificationEvent originalEvent : testEvents) {
            // When: Serialize and deserialize the event
            String json = objectMapper.writeValueAsString(originalEvent);
            NotificationEvent deserializedEvent = objectMapper.readValue(json, NotificationEvent.class);
            
            // Then: Should preserve all data correctly
            assertAll(
                () -> assertEquals(originalEvent.getId(), deserializedEvent.getId()),
                () -> assertEquals(originalEvent.getType(), deserializedEvent.getType()),
                () -> assertEquals(originalEvent.getMessage(), deserializedEvent.getMessage()),
                () -> assertEquals(originalEvent.getUsername(), deserializedEvent.getUsername()),
                () -> assertEquals(originalEvent.getTaskId(), deserializedEvent.getTaskId()),
                () -> assertEquals(originalEvent.getTaskTitle(), deserializedEvent.getTaskTitle()),
                () -> assertEquals(originalEvent.getTimestamp(), deserializedEvent.getTimestamp())
            );
        }
    }
    
    @Test
    @DisplayName("Should maintain message ordering in end-to-end flow")
    void testMessageOrdering() throws Exception {
        // Given: Multiple messages
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("testuser");
        
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(123L);
        when(task.getTitle()).thenReturn("Ordering Test Task");        when(task.getCreatedBy()).thenReturn(user);
        when(task.getAssignedTo()).thenReturn(user);
        
        // Create multiple events with sufficient timing differences
        NotificationEvent event1 = notificationFactory.createTaskCreatedNotification(task, user);
        Thread.sleep(1000); // Ensure different timestamps (1 second difference)
        NotificationEvent event2 = notificationFactory.createTaskUpdatedNotification(task, user);
        Thread.sleep(1000);
        NotificationEvent event3 = notificationFactory.createTaskDeletedNotification(task, user);
        
        // When: Serialize all events
        String json1 = objectMapper.writeValueAsString(event1);
        String json2 = objectMapper.writeValueAsString(event2);
        String json3 = objectMapper.writeValueAsString(event3);
        
        // When: Deserialize all events
        NotificationEvent deserialized1 = objectMapper.readValue(json1, NotificationEvent.class);
        NotificationEvent deserialized2 = objectMapper.readValue(json2, NotificationEvent.class);
        NotificationEvent deserialized3 = objectMapper.readValue(json3, NotificationEvent.class);
        
        // Then: Timestamps should be preserved and ordering maintained
        assertAll(
            () -> assertTrue(deserialized1.getTimestamp().isBefore(deserialized2.getTimestamp())),
            () -> assertTrue(deserialized2.getTimestamp().isBefore(deserialized3.getTimestamp())),
            () -> assertEquals(NotificationFactory.NotificationType.TASK_CREATED, deserialized1.getType()),
            () -> assertEquals(NotificationFactory.NotificationType.TASK_UPDATED, deserialized2.getType()),
            () -> assertEquals(NotificationFactory.NotificationType.TASK_DELETED, deserialized3.getType())
        );
    }
    
    @Test
    @DisplayName("Should handle high volume of messages without data corruption")
    void testHighVolumeMessaging() throws Exception {
        // Given: Many notification events
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("volumeuser");
          Task task = mock(Task.class);
        when(task.getId()).thenReturn(555L);
        when(task.getTitle()).thenReturn("Volume Test Task");
        // Note: We don't need to stub getCreatedBy() and getAssignedTo() for createTaskUpdatedNotification
        
        int messageCount = 100;
        
        // When: Create and serialize many events
        for (int i = 0; i < messageCount; i++) {
            NotificationEvent originalEvent = notificationFactory.createTaskUpdatedNotification(task, user);
            
            // Serialize and deserialize
            String json = objectMapper.writeValueAsString(originalEvent);
            NotificationEvent deserializedEvent = objectMapper.readValue(json, NotificationEvent.class);
            
            // Then: Each message should be preserved correctly
            assertAll(
                () -> assertEquals(originalEvent.getId(), deserializedEvent.getId()),
                () -> assertEquals(originalEvent.getType(), deserializedEvent.getType()),
                () -> assertEquals(originalEvent.getMessage(), deserializedEvent.getMessage()),
                () -> assertEquals(originalEvent.getUsername(), deserializedEvent.getUsername()),
                () -> assertEquals("555", deserializedEvent.getTaskId()),
                () -> assertEquals("Volume Test Task", deserializedEvent.getTaskTitle())
            );
        }
    }
}
