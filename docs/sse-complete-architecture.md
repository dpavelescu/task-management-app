# SSE Real-Time Architecture Documentation

## Overview

This document provides a comprehensive view of the Server-Sent Events (SSE) architecture in the Todo application, showcasing how real-time notifications are delivered across multiple browser tabs, devices, and backend pods while maintaining user-specific message segregation.

## Key Capabilities

- **Multi-Tab Support**: Users can have multiple browser tabs open and receive notifications on all tabs simultaneously
- **Cross-Pod Communication**: Backend pods communicate via Redis Pub/Sub to ensure notifications reach all user connections
- **User Segregation**: Each user only receives notifications relevant to them
- **Connection Resilience**: Automatic reconnection with event replay using Last-Event-ID
- **Load Balancer Compatible**: Works seamlessly behind Nginx load balancer with sticky sessions
- **Thread-Safe**: Concurrent connection management without blocking operations

---

## Mermaid Architecture Diagrams

### 1. High-Level System Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        B1[Browser Tab 1<br/>User A]
        B2[Browser Tab 2<br/>User A] 
        B3[Mobile App<br/>User B]
    end
    
    subgraph "Load Balancer"
        LB[Nginx<br/>Port 8090]
    end
    
    subgraph "Backend Pods"
        P1[Pod 1<br/>Port 8082]
        P2[Pod 2<br/>Port 8083]
        P3[Pod 3<br/>Port 8084]
    end
    
    subgraph "Messaging"
        R[Redis Pub/Sub<br/>Cross-Pod Messaging]
    end
    
    B1 -.->|SSE Connection| LB
    B2 -.->|SSE Connection| LB
    B3 -.->|SSE Connection| LB
    
    LB --> P1
    LB --> P2
    LB --> P3
    
    P1 <--> R
    P2 <--> R
    P3 <--> R
```

### 2. SSE Connection Flow

```mermaid
sequenceDiagram
    participant Browser
    participant Nginx
    participant Pod
    participant SSEManager
    participant Redis
    
    Browser->>+Nginx: SSE Request + JWT Token
    Nginx->>+Pod: Forward Request
    Pod->>+SSEManager: Create Connection
    SSEManager->>Pod: Validate JWT
    Pod->>SSEManager: Username Extracted
    SSEManager->>SSEManager: Store Connection
    SSEManager->>-Pod: SSE Emitter
    Pod->>-Nginx: Event Stream
    Nginx->>-Browser: Event Stream
    
    Note over Redis: Task Created Event
    Redis-->>SSEManager: Notification Message
    SSEManager->>Browser: Notification Event
```

### 3. User Connection Segregation

```mermaid
graph LR
    subgraph "Pod Instance"
        subgraph "SSEConnectionManager"
            subgraph "User Connections Map"
                UA[User A] --> UCA["[Emitter1, Emitter2, Emitter3]"]
                UB[User B] --> UCB["[Emitter4]"]
                UC[User C] --> UCC["[Emitter5, Emitter6]"]
            end
            
            subgraph "Recent Notifications Map"
                UA --> RNA["[Event1, Event2, Event3,...]"]
                UB --> RNB["[Event4, Event5,...]"]
                UC --> RNC["[Event6, Event7, Event8,...]"]
            end
        end
    end
```

### 4. Cross-Pod Notification Flow

```mermaid
graph TD
    subgraph "Notification Origin"
        TaskEvent[Task Created/Updated]
    end
    
    subgraph "Source Pod"
        NS[Notification Service]
        MP[Message Publisher]
    end
    
    subgraph "Redis"
        PS[Pub/Sub Channel<br/>task-notifications]
    end
    
    subgraph "Target Pods"
        subgraph "Pod 1"
            RC1[Redis Consumer]
            SM1[SSE Manager]
            UC1[User Connections]
        end
        
        subgraph "Pod 2"
            RC2[Redis Consumer]
            SM2[SSE Manager]
            UC2[User Connections]
        end
        
        subgraph "Pod 3"
            RC3[Redis Consumer]
            SM3[SSE Manager]
            UC3[User Connections]
        end
    end
    
    TaskEvent --> NS
    NS --> MP
    MP --> PS
    
    PS --> RC1
    PS --> RC2
    PS --> RC3
    
    RC1 --> SM1
    RC2 --> SM2
    RC3 --> SM3
    
    SM1 --> UC1
    SM2 --> UC2
    SM3 --> UC3
```

### 5. Connection Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Connecting: Browser initiates SSE
    Connecting --> Connected: Authentication successful
    Connecting --> Failed: Authentication failed
    
    Connected --> Receiving: Normal operation
    Receiving --> Connected: Event received
    
    Connected --> Disconnected: Network error
    Connected --> Disconnected: Client closed
    Connected --> Disconnected: Server error
    
    Disconnected --> Reconnecting: Auto-reconnect (5s delay)
    Reconnecting --> Connected: Reconnection successful
    Reconnecting --> Failed: Reconnection failed
      Failed --> [*]
    Disconnected --> [*]: Manual disconnect
```

### 6. Multi-Tab User Experience

```mermaid
graph TB
    subgraph "User A Sessions"
        T1[Browser Tab 1]
        T2[Browser Tab 2]
        M1[Mobile App]
    end
    
    subgraph "Backend Infrastructure"
        LB[Load Balancer]
        P1[Pod 1]
        P2[Pod 2]
        P3[Pod 3]
        Redis[Redis Pub/Sub]
    end
    
    subgraph "Notification Flow"
        Event[Task Created]
        Notify[Broadcast to All User A Connections]
    end
    
    T1 -.->|SSE Connection| LB
    T2 -.->|SSE Connection| LB
    M1 -.->|SSE Connection| LB
    
    LB --> P1
    LB --> P2
    LB --> P3
    
    P1 <--> Redis
    P2 <--> Redis
    P3 <--> Redis
    
    Event --> Notify
    Notify --> T1
    Notify --> T2
    Notify --> M1
```

### 7. Last-Event-ID Replay Mechanism

```mermaid
sequenceDiagram
    participant Client
    participant Server
    participant EventStore
    
    Note over Client, EventStore: Normal Operation
    Client->>Server: SSE Connection
    Server->>Client: Event 1 (id: 1)
    Server->>Client: Event 2 (id: 2)
    Server->>Client: Event 3 (id: 3)
    Server->>Client: Event 4 (id: 4)
    Server->>Client: Event 5 (id: 5)
    
    Note over Client: Connection Lost
    Server->>EventStore: Store Event 6 (id: 6)
    Server->>EventStore: Store Event 7 (id: 7)
    Server->>EventStore: Store Event 8 (id: 8)
    
    Note over Client, EventStore: Reconnection with Replay
    Client->>Server: Reconnect with Last-Event-ID: 5
    Server->>EventStore: Query events after ID 5
    EventStore->>Server: Return [6, 7, 8]
    Server->>Client: Replay Event 6 (id: 6)
    Server->>Client: Replay Event 7 (id: 7)
    Server->>Client: Replay Event 8 (id: 8)
    Server->>Client: Resume normal operation
```

### 8. Thread Safety Architecture

```mermaid
graph TB
    subgraph "Thread Safety Model"
        subgraph "ConcurrentHashMap"
            K1[Key: User A] --> V1[CopyOnWriteArrayList<br/>[Emitter1, Emitter2, Emitter3]]
            K2[Key: User B] --> V2[CopyOnWriteArrayList<br/>[Emitter4]]
            K3[Key: User C] --> V3[CopyOnWriteArrayList<br/>[Emitter5, Emitter6]]
        end
        
        subgraph "Concurrent Operations"
            T1[Thread 1<br/>Read/Write]
            T2[Thread 2<br/>Read/Write]
            T3[Thread 3<br/>Read/Write]
        end
        
        subgraph "Benefits"
            B1[High Concurrency]
            B2[No Blocking]
            B3[Consistent Reads]
            B4[Atomic Updates]
        end
    end
    
    T1 --> K1
    T1 --> K2
    T2 --> K2
    T2 --> K3
    T3 --> K1
    T3 --> K3
    
    K1 --> B1
    K2 --> B2
    K3 --> B3
    V1 --> B4
```

### 9. Memory Management Strategy

```mermaid
graph TB
    subgraph "Memory Management"
        subgraph "Recent Notifications Cache"
            UA[User A Cache<br/>MAX_SIZE: 100]
            UB[User B Cache<br/>MAX_SIZE: 100]
            UC[User C Cache<br/>MAX_SIZE: 100]
        end
        
        subgraph "Cache Behavior"
            FIFO[FIFO Eviction<br/>When cache full]
            NewEvent[New Event Arrives]
            OldEvent[Oldest Event Removed]
        end
        
        subgraph "Connection Cleanup"
            Active[Active Emitters<br/>Keep in Collection]
            Failed[Failed Emitters<br/>Remove from Collection]
            Closed[Closed Emitters<br/>Remove from Collection]
        end
        
        subgraph "Cleanup Triggers"
            OnComplete[onCompletion()]
            OnTimeout[onTimeout()]
            OnError[onError()]
            Periodic[Periodic Cleanup]
        end
    end
    
    NewEvent --> FIFO
    FIFO --> OldEvent
    
    Active --> OnComplete
    Failed --> OnTimeout
    Closed --> OnError
    
    OnComplete --> Periodic
    OnTimeout --> Periodic
    OnError --> Periodic
```

### 10. Error Handling Flow

```mermaid
graph TB
    subgraph "Client Side"
        ES[EventSource]
        ErrorState[Set Error State]
        ReconnectTimer[Start Reconnect Timer<br/>5 second delay]
        NewConnection[Create New EventSource]
    end
    
    subgraph "Server Side"
        SE[SseEmitter]
        RemoveConn[Remove Connection]
        LogError[Log Error]
    end
    
    subgraph "Error Types"
        NetError[Network Connectivity]
        ServerDown[Server Unavailability]
        TokenExp[Token Expiration]
        LBRouting[Load Balancer Changes]
        PodRestart[Pod Restarts]
    end
    
    ES -->|.onerror| ErrorState
    SE -->|.onError| RemoveConn
    
    ErrorState --> ReconnectTimer
    ReconnectTimer --> NewConnection
    RemoveConn --> LogError
    
    NetError --> ES
    ServerDown --> ES
    TokenExp --> ES
    LBRouting --> ES
    PodRestart --> ES
```

---

## Architecture Components

### Frontend (React)
- **useSimpleSSE Hook**: Manages SSE connections with automatic reconnection
- **EventSource API**: Browser-native SSE support with Last-Event-ID headers
- **Connection Status UI**: Visual indicators for connection state

### Backend (Spring Boot)
- **SSEConnectionManager**: Thread-safe connection management using ConcurrentHashMap
- **SseEmitter**: Spring's SSE implementation for maintaining persistent connections
- **NotificationService**: Business logic for determining which users receive notifications
- **Redis Messaging**: Cross-pod communication via Pub/Sub channels

### Infrastructure
- **Nginx Load Balancer**: Routes requests with sticky session support for SSE
- **Redis**: Provides cross-pod messaging and optional event persistence
- **Multi-Pod Deployment**: Horizontal scaling with shared state via Redis

---

## Key Benefits

1. **Real-Time Experience**: Instant notifications across all user devices and tabs
2. **Scalable Architecture**: Multiple backend pods can serve different users simultaneously
3. **Resilient Connections**: Automatic reconnection with event replay prevents message loss
4. **User Privacy**: Each user only receives their relevant notifications
5. **Load Balancer Compatible**: Works seamlessly behind standard load balancers
6. **Thread-Safe Design**: High concurrency without performance bottlenecks
7. **Memory Efficient**: Automatic cleanup of stale connections and bounded event storage

---

## Usage Instructions

### Viewing Mermaid Diagrams

To view the Mermaid diagrams:

1. **GitHub/GitLab**: Mermaid diagrams render automatically in markdown files
2. **VS Code**: Install "Mermaid Preview" extension
3. **Online**: Copy diagram code to https://mermaid.live/
4. **Documentation sites**: Most support Mermaid rendering (GitBook, Notion, etc.)

### Converting to Images

```bash
# Install mermaid CLI
npm install -g @mermaid-js/mermaid-cli

# Convert diagrams to images
mmdc -i sse-complete-architecture.md -o sse-architecture.png -t dark
```

This architecture provides a robust foundation for real-time web applications requiring immediate user notifications across multiple connections and devices.
