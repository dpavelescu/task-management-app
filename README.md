# Real-Time Task Management Application

A full-stack task management application with real-time notifications, built with Spring Boot and React.

## Features

- **User Management**: Registration, login with JWT authentication
- **Task Management**: Create, assign, update, and delete tasks
- **Real-Time Notifications**: Live updates using Server-Sent Events (SSE)
- **Task Assignment**: Assign tasks to specific users from registered user list
- **Task Views**: "My Tasks" (assigned to me) and "Managed Tasks" (created by me)
- **Task Status Tracking**: PENDING → IN_PROGRESS → COMPLETED workflow
- **Priority Management**: LOW, MEDIUM, HIGH, URGENT priorities

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL with JPA/Hibernate
- **Security**: JWT-based authentication with refresh tokens
- **Real-time**: Server-Sent Events (SSE)
- **Build Tool**: Maven

### Frontend
- **Framework**: React 18+ with TypeScript
- **Build Tool**: Vite
- **Styling**: Modern CSS with responsive design
- **State Management**: React Context API + Custom Hooks

### Infrastructure
- **Containerization**: Docker & Docker Compose
- **Deployment**: Helm charts for Kubernetes
- **Database**: PostgreSQL

## Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Docker Desktop
- Git

### 1. Clone Repository
```bash
git clone <repository-url>
cd ToDo
```

### 2. Start Backend & Database
```bash
cd docker
docker-compose up -d
```

### 3. Start Frontend
```bash
cd frontend
npm install
npm run dev
```

### 4. Access Application
- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **Database**: localhost:5432

## Project Structure

```
ToDo/
├── backend/                 # Spring Boot application
│   ├── src/main/java/com/taskapp/
│   │   ├── entity/         # JPA entities
│   │   ├── dto/            # Data transfer objects
│   │   ├── enums/          # TaskStatus, TaskPriority enums
│   │   ├── service/        # Business logic
│   │   ├── controller/     # REST endpoints
│   │   ├── repository/     # Data access layer
│   │   ├── security/       # Authentication & authorization
│   │   ├── exception/      # Custom exceptions
│   │   └── mapper/         # Entity-DTO conversions
│   └── src/main/resources/
│       ├── application.properties
│       ├── application-dev.properties
│       └── application-prod.properties
├── frontend/               # React TypeScript application
│   ├── src/
│   │   ├── components/     # Reusable UI components
│   │   ├── pages/          # Page components
│   │   ├── hooks/          # Custom React hooks
│   │   ├── contexts/       # React contexts
│   │   ├── api/            # HTTP client functions
│   │   ├── types/          # TypeScript interfaces
│   │   └── utils/          # Helper functions
│   └── package.json
├── db/                     # Database initialization
│   └── init/
│       └── 01_init.sql
├── docker/                 # Docker configuration
│   └── docker-compose.yml
├── helm/                   # Kubernetes Helm charts
│   ├── backend/
│   ├── database/
│   └── redis/
└── docs/                   # Documentation

```

## API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh JWT token

### Tasks
- `GET /api/tasks` - Get user's tasks
- `POST /api/tasks` - Create new task
- `PUT /api/tasks/{id}` - Update task
- `DELETE /api/tasks/{id}` - Delete task

### Users
- `GET /api/users` - Get all users (for task assignment)

### Notifications
- `GET /api/notifications/stream/{username}` - SSE endpoint for real-time notifications

## Development

### Backend Development
The backend runs in Docker for consistent development environment:

```bash
cd docker
docker-compose up -d

# View logs
docker-compose logs -f backend

# Rebuild after code changes
docker-compose build backend
docker-compose up -d backend
```

### Frontend Development
The frontend runs locally with hot module replacement:

```bash
cd frontend
npm run dev

# Build for production
npm run build
```

### Database Access
Connect to PostgreSQL:
- **Host**: localhost
- **Port**: 5432
- **Database**: taskapp
- **Username**: postgres
- **Password**: password

## Environment Configuration

### Development (`application-dev.properties`)
- Enhanced logging for debugging
- SQL query logging enabled
- Local database connection

### Production (`application-prod.properties`)
- Optimized logging (INFO level)
- No SQL query output
- Production database configuration

## Real-Time Architecture

The application uses a notification-trigger pattern:
1. Backend sends lightweight notification via SSE when tasks change
2. Frontend receives notification and fetches updated data via REST API
3. This ensures data consistency and reduces payload size

### Notification Types
- `TASK_CREATED` - New task created
- `TASK_ASSIGNED` - Task assigned to user
- `TASK_UPDATED` - Task modified
- `TASK_REASSIGNED` - Task reassigned to different user
- `TASK_DELETED` - Task deleted

## Deployment

### Local Development
```bash
docker-compose up -d
cd frontend && npm run dev
```

### Production (Kubernetes)
```bash
# Deploy backend
helm install taskapp-backend ./helm/backend

# Deploy database
helm install taskapp-db ./helm/database
```

## Testing

### Manual Testing Scripts
```bash
cd test-scripts

# Test backend health
./check-status.bat

# Test user registration
./test-backend.bat

# Test task operations
./test-task-deletion.bat
```

## Contributing

1. Follow the established patterns (enums for constants, factory pattern for notifications)
2. Use proper exception handling with custom business exceptions
3. Maintain separation between entities and DTOs
4. Write meaningful commit messages
5. Test real-time functionality across multiple browser tabs

## License

[Your License Here]