# Configuration Management Guide

## Overview
This document describes how configuration is managed across different environments for better maintainability and deployment flexibility.

## Backend Configuration

### Environment Profiles
The backend supports different environment profiles:

- **Development**: `application-dev.properties` - Verbose logging, SQL debugging
- **Production**: `application-prod.properties` - Optimized for performance
- **Main**: `application.properties` - Shared configuration

### Setting Active Profile
Use the Spring profile system:
```bash
# Development (default)
java -jar app.jar --spring.profiles.active=dev

# Production
java -jar app.jar --spring.profiles.active=prod

# Docker environment variable
SPRING_PROFILES_ACTIVE=dev
```

### Environment Variables (Production)
Production configuration supports environment variable overrides:

```bash
# Server
SERVER_PORT=8080

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/todo_db
DATABASE_USERNAME=todo_user
DATABASE_PASSWORD=todo_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your_very_long_and_secure_jwt_secret_key_here
JWT_EXPIRATION=86400000

# CORS
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
CORS_ALLOW_CREDENTIALS=true

# Multi-pod deployment
MESSAGING_POD_ID=backend-1
```

### CORS Configuration
CORS origins are now configurable via:
- `app.cors.allowed-origins` property (comma-separated list)
- `app.cors.allow-credentials` property (boolean)

## Frontend Configuration

### Environment Files
The frontend uses Vite's environment system:

- **Development**: `.env.development`
- **Production**: `.env.production`  
- **Local Override**: `.env.local`

### Environment Variables
```bash
# API Configuration
VITE_API_URL=http://localhost:8080/api
VITE_SSE_URL=http://localhost:8080/api/notifications/stream
VITE_ENVIRONMENT=development
```

### Build Configuration
For production builds:
```bash
# Set environment variables before build
export VITE_API_URL=https://api.yourdomain.com/api
export VITE_SSE_URL=https://api.yourdomain.com/api/notifications/stream

npm run build
```

## Test Scripts Configuration

### Configuration File
Test scripts now use `test-scripts/config.bat` for centralized configuration:

```batch
set BACKEND_URL=http://localhost:8080/api
set BACKEND_HOST=localhost
set BACKEND_PORT=8080
set FRONTEND_URL=http://localhost:5173
set FRONTEND_PORT=5173
set DATABASE_PORT=5432
set REDIS_PORT=6379
```

### Usage
All test scripts automatically load this configuration:
```batch
call "%~dp0config.bat"
```

## Docker Configuration

### Environment Variables
Docker Compose now supports environment variable overrides:

```bash
# Create .env file in docker directory
DATABASE_PORT=5432
REDIS_PORT=6379
POSTGRES_DB=todo_db
POSTGRES_USER=todo_user
POSTGRES_PASSWORD=todo_password
```

### Docker Compose Usage
```bash
# Use environment variables
docker-compose --env-file .env up

# Override specific variables
DATABASE_PORT=5433 docker-compose up
```

## Configuration Files Summary

### Backend
- `backend/src/main/resources/application.properties` - Main configuration
- `backend/src/main/resources/application-dev.properties` - Development
- `backend/src/main/resources/application-prod.properties` - Production
- `backend/src/main/java/com/taskapp/config/CorsProperties.java` - CORS configuration class

### Frontend
- `frontend/.env.development` - Development environment
- `frontend/.env.production` - Production environment  
- `frontend/.env.local` - Local overrides
- `frontend/vite.config.ts` - Build configuration with proxy

### Test Scripts
- `test-scripts/config.bat` - Centralized test configuration

### Docker
- `docker/.env.example` - Environment template
- `docker/docker-compose.yml` - Updated with variable support

## Deployment Examples

### Development
```bash
# Backend
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend  
cd frontend
npm run dev
```

### Production
```bash
# Backend with environment variables
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://prod-db:5432/todo_db
export CORS_ALLOWED_ORIGINS=https://yourdomain.com
java -jar backend.jar

# Frontend build with production API
export VITE_API_URL=https://api.yourdomain.com/api
npm run build
```

### Docker Production
```bash
# Create production .env file
echo "DATABASE_PORT=5432" > docker/.env
echo "POSTGRES_PASSWORD=secure_password" >> docker/.env

# Deploy
docker-compose --env-file docker/.env up -d
```

## Security Notes

1. **Never commit `.env.local` or production `.env` files**
2. **Use strong, unique JWT secrets in production**
3. **Restrict CORS origins to your actual domains**
4. **Use environment variables for sensitive configuration**
5. **Rotate secrets regularly**

## Migration from Hardcoded Values

The following hardcoded values have been externalized:

### Backend
- ✅ CORS origins: `http://localhost:3000,http://localhost:5173` → `app.cors.allowed-origins`
- ✅ Server port: `8080` → `server.port`
- ✅ Database URL: `jdbc:postgresql://localhost:5432/todo_db` → `spring.datasource.url`

### Frontend  
- ✅ API URL: `http://localhost:8080/api` → `VITE_API_URL`
- ✅ SSE URL: `http://localhost:8080/api/notifications/stream` → `VITE_SSE_URL`

### Test Scripts
- ✅ All localhost URLs and ports → `config.bat` variables

### Docker
- ✅ Database ports and credentials → environment variables

All systems now support flexible configuration for different environments while maintaining backward compatibility with sensible defaults.
