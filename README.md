# Real-Time Task Management Application

A full-stack task management application with real-time notifications, built with Spring Boot and React. Features multi-pod deployment support with Redis pub/sub messaging for scalable real-time notifications.

## Features

- **User Management**: Registration, login with JWT authentication
- **Task Management**: Create, assign, update, and delete tasks
- **Real-Time Notifications**: Live updates using Server-Sent Events (SSE)
- **Task Assignment**: Assign tasks to specific users from registered user list
- **Task Views**: "My Tasks" (assigned to me) and "Managed Tasks" (created by me)
- **Task Status Tracking**: PENDING → IN_PROGRESS → COMPLETED workflow
- **Priority Management**: LOW, MEDIUM, HIGH, URGENT priorities
- **Multi-Pod Support**: Scalable deployment with Redis pub/sub messaging

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL with JPA/Hibernate
- **Security**: JWT-based authentication with refresh tokens
- **Real-time**: Server-Sent Events (SSE) with Redis pub/sub
- **Build Tool**: Maven

### Frontend
- **Framework**: React 18+ with TypeScript
- **Build Tool**: Vite
- **Styling**: Modern CSS with responsive design
- **State Management**: React Context API + Custom Hooks

### Infrastructure
- **Containerization**: Docker & Docker Compose
- **Database**: PostgreSQL
- **Messaging**: Redis pub/sub for real-time notifications
- **Load Balancing**: Nginx for multi-pod deployments

## Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Docker Desktop
- Git

### 1. Clone Repository
```bash
git clone <repository-url>
cd task-management-app
```

### 2. Start Application (Single Instance)
```cmd
npm run dev
```
This starts both backend (Docker with auto-build) and frontend (local dev server).

### 3. Start Multi-Pod Deployment
```cmd
npm run multi-pod:start
```
This automatically builds the backend Docker image and starts multiple backend instances with load balancing.

#### Manual Build (Optional)
For custom builds or development:
```cmd
# Backend compilation (optional - Docker handles this)
cd backend && mvn clean package -DskipTests && cd ..

# Frontend build (for production)
cd frontend && npm run build && cd ..
```

### 4. Access Application
- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **Load Balanced API** (multi-pod): http://localhost:8090
- **Database**: localhost:5432

## Documentation

- 📖 [Configuration Guide](docs/configuration-guide.md) - Environment setup and configuration
- 🚀 [Multi-Pod Deployment](docs/multi-pod-deployment.md) - Scalable deployment guide
- 🧪 [API Testing](test-scripts/README.md) - API testing and validation
- 🛠️ [Backend Documentation](backend/README.md) - Backend API and development
- ⚛️ [Frontend Documentation](frontend/README.md) - React application setup

## Project Structure

```
task-management-app/
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
│   │   ├── mapper/         # Entity-DTO conversions
│   │   └── config/         # Spring configuration
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
│   │   ├── config/         # Configuration
│   │   └── utils/          # Helper functions
│   └── package.json
├── db/                     # Database initialization
│   └── init/
│       └── 01_init.sql
├── docker/                 # Docker configuration
│   ├── docker-compose.yml           # Single instance
│   ├── docker-compose.multi-pod.yml # Multi-pod deployment
│   └── nginx-multi-pod.conf         # Load balancer config
├── scripts/                # Deployment scripts
│   ├── deploy-docker.bat           # Windows deployment
│   └── test-multi-pod.bat          # Multi-pod testing
├── test-scripts/           # API testing
│   ├── api-tests.http
│   └── sse-tests.http
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

### Available Commands
```bash
# Start development environment
npm run dev                    # Backend (Docker) + Frontend (local)

# Multi-pod deployment
npm run multi-pod:start        # Start multi-pod environment
npm run multi-pod:test         # Test multi-pod functionality
npm run multi-pod:logs         # View container logs
npm run multi-pod:stop         # Stop multi-pod environment

# Development utilities
npm run dev:logs               # View backend logs
npm run restart                # Restart backend container
npm run status                 # Check service status
npm run clean                  # Clean Docker volumes and images
```

### Backend Development
The backend runs in Docker for consistent development environment:

```bash
# View logs
npm run dev:logs

# Rebuild after code changes
npm run restart
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
- **Database**: todo_db
- **Username**: todo_user
- **Password**: todo_password

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

The application uses a notification-trigger pattern with Redis pub/sub for multi-pod scaling:
1. Backend publishes lightweight notification via Redis when tasks change
2. All backend instances receive the notification via Redis subscription
3. Frontend receives notification via SSE and fetches updated data via REST API
4. This ensures data consistency and supports horizontal scaling

### Notification Types
- `TASK_CREATED` - New task created
- `TASK_ASSIGNED` - Task assigned to user
- `TASK_UPDATED` - Task modified
- `TASK_REASSIGNED` - Task reassigned to different user
- `TASK_DELETED` - Task deleted

## Deployment

### Single Instance (Development)
```bash
npm run dev
```

### Multi-Pod (Production-like)
```bash
npm run multi-pod:start
```

Access points:
- **Load Balanced**: http://localhost:8090
- **Backend Pod 1**: http://localhost:8082
- **Backend Pod 2**: http://localhost:8083

## Testing

### Manual Testing
```bash
# API testing with VS Code REST Client
# Open test-scripts/api-tests.http

# SSE testing
# Open test-scripts/sse-tests.http
```

### Multi-Pod Testing
```bash
npm run multi-pod:test
```

## Contributing

1. Follow the established patterns (enums for constants, factory pattern for notifications)
2. Use proper exception handling with custom business exceptions
3. Maintain separation between entities and DTOs
4. Write meaningful commit messages
5. Test real-time functionality across multiple browser tabs

## License

MIT License