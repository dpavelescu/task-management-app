# Backend - Spring Boot Task Management API

This is the backend component of the Real-Time Task Management Application, built with Spring Boot 3.x.

## Overview

- **Framework**: Spring Boot 3.x with Spring Security
- **Database**: PostgreSQL with JPA/Hibernate
- **Authentication**: JWT-based with refresh tokens
- **Real-time**: Server-Sent Events (SSE) with Redis pub/sub
- **Build Tool**: Maven

## Project Structure

```
src/main/java/com/taskapp/
├── config/          # Spring configuration classes
├── controller/      # REST API endpoints
├── dto/             # Data Transfer Objects
├── entity/          # JPA entities
├── enums/           # TaskStatus, TaskPriority, NotificationType
├── exception/       # Custom exceptions and global handler
├── mapper/          # Entity-DTO mapping
├── repository/      # JPA repositories
└── service/         # Business logic and messaging
```

## Key Features

### Authentication & Authorization
- JWT token-based authentication
- Refresh token support for seamless user experience
- Password encryption with BCrypt
- Permission-based task operations

### Real-Time Messaging
- Server-Sent Events (SSE) for real-time notifications
- Redis pub/sub for multi-pod message distribution
- Notification factory pattern for type-safe message creation
- Username-based message routing

### Task Management
- CRUD operations for tasks
- Task assignment to users
- Status workflow: PENDING → IN_PROGRESS → COMPLETED
- Priority levels: LOW, MEDIUM, HIGH, URGENT

## Configuration

### Profiles
- `dev` - Development profile with verbose logging
- `prod` - Production profile with optimized settings

### Key Properties
```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/todo_db
spring.datasource.username=todo_user
spring.datasource.password=todo_password

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT
app.jwt.secret=your-secret-key
app.jwt.expiration=86400000

# CORS
app.cors.allowed-origins=http://localhost:5173
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh JWT token

### Tasks
- `GET /api/tasks` - Get user's tasks (created or assigned)
- `POST /api/tasks` - Create new task
- `PUT /api/tasks/{id}` - Update task
- `DELETE /api/tasks/{id}` - Delete task

### Users
- `GET /api/users` - Get all users (for task assignment)

### Notifications
- `GET /api/notifications/stream/{username}` - SSE endpoint for real-time notifications

### Monitoring
- `GET /actuator/health` - Health check endpoint
- `GET /actuator/info` - Application information

## Development

### Running Locally
```bash
# With Docker (recommended)
cd docker
docker-compose up -d

# Direct Maven run
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Building

#### Local Development Build
```cmd
# Full build with tests
mvn clean package

# Quick build (skip tests)
mvn clean package -DskipTests

# Compile only (no packaging)
mvn compile
```

#### Docker Image Build
```cmd
# Build Docker image (includes Maven compilation)
docker build -t taskapp/backend:latest .

# Build with custom tag
docker build -t taskapp/backend:v1.0.0 .
```

#### Production Build
```cmd
# Build with production profile
mvn clean package -Pprod -DskipTests

# Build Docker image for production
docker build --build-arg SPRING_PROFILES_ACTIVE=prod -t taskapp/backend:prod .
```

**Build Output**: 
- JAR file: `target/taskapp-backend-0.0.1-SNAPSHOT.jar`
- Docker image: `taskapp/backend:latest`

### Testing
```cmd
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=TaskServiceTest

# Run tests with specific profile
mvn test -Dspring.profiles.active=test

# Generate test coverage report
mvn clean test jacoco:report
```

## Multi-Pod Deployment

For horizontal scaling, the backend supports multi-pod deployment:

- Each pod requires a unique `MESSAGING_POD_ID` environment variable
- Redis pub/sub handles cross-pod message distribution
- Load balancer (Nginx) distributes requests with sticky sessions for SSE

See [Multi-Pod Deployment Guide](../docs/multi-pod-deployment.md) for details.

## Logging

Structured logging with different levels per environment:
- Development: DEBUG level with SQL logging
- Production: INFO level, optimized for performance

Log files are written to `logs/` directory with daily rotation.

## Security

- JWT tokens with configurable expiration
- CORS configured for allowed origins
- SQL injection protection via JPA
- Password hashing with BCrypt
- Input validation on all endpoints