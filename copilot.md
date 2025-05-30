# Copilot Project Instructions: Real-Time Task Management App

## Project Overview
Full-stack task management application with real-time notifications. Users can create tasks, assign them to other users, and receive real-time updates when tasks are assigned, updated, or deleted.

## Architecture

### Backend (Spring Boot)
- **Framework**: Spring Boot 3.x with Spring Security
- **Database**: PostgreSQL with JPA/Hibernate
- **Real-time**: Server-Sent Events (SSE) for notifications
- **Authentication**: JWT-based with refresh tokens
- **Logging**: Structured logging with different levels per environment

### Frontend (React + TypeScript)
- **Framework**: React 18+ with TypeScript, Vite build tool
- **State**: Context API for auth, custom hooks for data management
- **Real-time**: EventSource for SSE consumption
- **Styling**: Modern CSS with responsive design

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
- Types: TASK_CREATED, TASK_ASSIGNED, TASK_UPDATED, TASK_REASSIGNED, TASK_DELETED
- Username-based routing for targeted notifications

## Key Implementation Details

### Backend Structure
```
src/main/java/com/taskapp/
├── entity/          # JPA entities
├── dto/             # Data transfer objects
├── enums/           # TaskStatus, TaskPriority
├── repository/      # JPA repositories
├── service/         # Business logic
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
└── utils/           # Helper functions
```

### Environment Configuration
- **Development**: Verbose logging, SQL logging enabled, local database
- **Production**: INFO level logging, no SQL output, optimized for performance

## Development Workflow
1. Backend runs in Docker container with PostgreSQL
2. Frontend runs locally with HMR for development speed
3. SSE endpoint: `/api/notifications/stream/{username}`
4. REST endpoints: `/api/auth/*`, `/api/tasks/*`, `/api/users/*`

## Security Implementation
- JWT access tokens (short-lived) + refresh tokens
- Password encryption with BCrypt
- Permission-based task operations (creator/assignee validation)
- CORS configured for local development

## Deployment Architecture
- **Local**: Docker Compose (backend + DB), local frontend
- **Production**: Helm charts for backend components, separate frontend deployment

This codebase demonstrates modern Spring Boot and React patterns with clean architecture, proper separation of concerns, and production-ready logging and security practices.
