# Copilot Project Instructions: Real-Time Task Management App

## Project Overview
Full-stack task management application with real-time notifications and multi-pod scaling support. Users can create tasks, assign them to other users, and receive real-time updates when tasks are assigned, updated, or deleted. The application supports horizontal scaling through Redis pub/sub messaging.

## Architecture

### Backend (Spring Boot)
- **Framework**: Spring Boot 3.x with Spring Security
- **Database**: PostgreSQL with JPA/Hibernate
- **Real-time**: Server-Sent Events (SSE) for notifications
- **Messaging**: Redis pub/sub for multi-pod communication
- **Authentication**: JWT-based with refresh tokens
- **Logging**: Structured logging with different levels per environment

### Frontend (React + TypeScript)
- **Framework**: React 18+ with TypeScript, Vite build tool
- **State**: Context API for auth, custom hooks for data management
- **Real-time**: EventSource for SSE consumption
- **Styling**: Modern CSS with responsive design

### Infrastructure
- **Deployment**: Docker Compose (single and multi-pod configurations)
- **Load Balancing**: Nginx for multi-pod deployments
- **Database**: PostgreSQL
- **Messaging**: Redis for pub/sub communication

### Key Design Patterns
- **Notification Factory**: Centralized notification creation with builder pattern
- **Task Mapper**: Entity-DTO conversions with proper encapsulation
- **Permission Helper**: Centralized authorization logic
- **Enums**: Type-safe status and priority management (TaskStatus, TaskPriority)
- **Custom Exceptions**: Specific business exceptions (TaskPermissionException, TaskAssignmentException)

## Data Model

### Core Entities
- **User**: id, username, email, password (encrypted)
- **Task**: id, title, description, status (enum), priority (enum), createdBy, assignedTo, timestamps

### Task Status Flow
- PENDING → IN_PROGRESS → COMPLETED
- CANCELLED (from any state)

### Task Priorities
- LOW, MEDIUM, HIGH, URGENT

## Real-time Notification Strategy
- Notifications are lightweight triggers (no full data payload)
- Frontend receives notification event → fetches fresh data via REST API
- Redis pub/sub enables cross-pod message delivery for horizontal scaling
- Types: TASK_CREATED, TASK_ASSIGNED, TASK_UPDATED, TASK_REASSIGNED, TASK_DELETED
- Username-based routing for targeted notifications

## Multi-Pod Architecture
- Multiple backend instances behind Nginx load balancer
- Redis pub/sub for cross-pod real-time message delivery
- Sticky sessions for SSE connections (IP hash load balancing)
- Each pod has unique MESSAGING_POD_ID for message routing
- Health checks and graceful degradation

## Key Implementation Details

### Backend Structure
```
src/main/java/com/taskapp/
├── entity/          # JPA entities
├── dto/             # Data transfer objects
├── enums/           # TaskStatus, TaskPriority
├── repository/      # JPA repositories
├── service/         # Business logic + messaging
├── controller/      # REST endpoints
├── security/        # JWT auth configuration
├── exception/       # Custom exceptions + global handler
├── mapper/          # Entity-DTO conversions
└── config/          # Spring configuration
```

### Frontend Structure
```
src/
├── components/      # Reusable UI components
├── pages/           # Page-level components
├── hooks/           # Custom hooks (auth, SSE, task management)
├── contexts/        # React contexts (auth)
├── api/             # HTTP client functions
├── types/           # TypeScript interfaces
├── config/          # Configuration management
└── utils/           # Helper functions
```

### Deployment Structure
```
docker/
├── docker-compose.yml           # Single instance
├── docker-compose.multi-pod.yml # Multi-pod deployment
└── nginx-multi-pod.conf         # Load balancer config

scripts/
├── deploy-docker.bat           # Windows deployment
└── test-multi-pod.bat          # Multi-pod testing
```

### Environment Configuration
- **Development**: Verbose logging, SQL logging enabled, local database
- **Production**: INFO level logging, no SQL output, optimized for performance

## Development Workflow
1. Backend runs in Docker container with PostgreSQL and Redis
2. Frontend runs locally with HMR for development speed
3. Multi-pod deployment for testing horizontal scaling
4. SSE endpoint: `/api/notifications/stream/{username}`
5. REST endpoints: `/api/auth/*`, `/api/tasks/*`, `/api/users/*`

## Security Implementation
- JWT access tokens (short-lived) + refresh tokens
- Password encryption with BCrypt
- Permission-based task operations (creator/assignee validation)
- CORS configured for development and production environments

## Deployment Options
- **Development**: Single instance via `npm run dev`
- **Multi-Pod Testing**: `npm run multi-pod:start` with load balancing
- **Configuration**: Environment-based (dev/prod profiles)
- **Scripts**: Automated deployment and testing scripts

This codebase demonstrates modern Spring Boot and React patterns with clean architecture, proper separation of concerns, production-ready logging and security practices, and horizontal scaling capabilities.
