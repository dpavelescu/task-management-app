# Documentation Index

This directory contains comprehensive documentation for the Real-Time Task Management Application.

## Quick Navigation

### 🚀 Getting Started
- [Main README](../README.md) - Project overview and quick start
- [Configuration Guide](configuration-guide.md) - Environment setup and configuration

### 🏗️ Deployment
- [Multi-Pod Deployment](multi-pod-deployment.md) - Scalable deployment with load balancing

### 📚 Component Documentation
- [Backend README](../backend/README.md) - Backend API, configuration, and development
- [Frontend README](../frontend/README.md) - React application setup and features

### 🧪 Testing
- [API Testing](../test-scripts/README.md) - API testing and validation scripts

## Architecture Overview

The application follows a modern microservices architecture with:

- **Frontend**: React + TypeScript with Vite
- **Backend**: Spring Boot with JWT authentication  
- **Database**: PostgreSQL
- **Messaging**: Redis pub/sub for real-time notifications
- **Deployment**: Docker Compose with multi-pod support

## Key Features

- ✅ Real-time notifications via Server-Sent Events (SSE)
- ✅ Multi-pod horizontal scaling with Redis pub/sub
- ✅ JWT-based authentication with refresh tokens
- ✅ Load balancing with Nginx
- ✅ Environment-based configuration
- ✅ Comprehensive API testing suite

## Build Process

### Quick Build (Automated)
```cmd
# Single instance deployment (includes backend build)
npm run dev

# Multi-pod deployment (includes Docker image build)
npm run multi-pod:start
```

### Manual Build Process

#### Backend Build
```cmd
cd backend
# Compile and package
mvn clean package -DskipTests
# Build Docker image
docker build -t taskapp/backend:latest .
cd ..
```

#### Frontend Build
```cmd
cd frontend
npm install
# Development server
npm run dev
# Production build
npm run build
cd ..
```

For detailed build instructions, see:
- [Backend Build Guide](../backend/README.md#building)
- [Frontend Build Guide](../frontend/README.md#building)

## Contributing

When updating documentation:

1. **Keep it current** - Ensure all examples and configurations match the actual codebase
2. **Link properly** - Use relative links between documentation files
3. **Test examples** - Verify all command examples work correctly
4. **Update main README** - Keep the main README as a navigation hub

## Documentation Structure

```
docs/
├── README.md                    # This file - documentation index
├── configuration-guide.md       # Environment and configuration setup
└── multi-pod-deployment.md     # Scalable deployment guide
```

For development context and AI assistance, see [copilot.md](../copilot.md) in the root directory.