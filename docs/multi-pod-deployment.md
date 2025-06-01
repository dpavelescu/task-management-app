# Multi-Pod Deployment Guide

This guide explains how to deploy and test the TaskApp with multiple backend instances for cross-pod SSE (Server-Sent Events) scaling using Redis pub/sub messaging.

## Prerequisites

- Docker Desktop installed and running
- Docker Compose available
- At least 4GB RAM available for containers
- Windows command prompt or PowerShell

## Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Load Balancer │
│  (React/Vite)   │    │     (Nginx)     │
└─────────────────┘    └─────────────────┘
         │                       │
         └───────────────────────┼───────────────────────┐
                                 │                       │
                    ┌─────────────────┐    ┌─────────────────┐
                    │   Backend Pod 1 │    │   Backend Pod 2 │
                    │  (Spring Boot)  │    │  (Spring Boot)  │
                    └─────────────────┘    └─────────────────┘
                                 │                       │
                                 └───────────┬───────────┘
                                             │
                              ┌─────────────────────────────┐
                              │     Redis Pub/Sub           │
                              │   (Message Broker)          │
                              └─────────────────────────────┘
                                             │
                              ┌─────────────────────────────┐
                              │      PostgreSQL             │
                              │      (Database)             │
                              └─────────────────────────────┘
```

## Quick Start

### Docker Compose Deployment (Recommended for Development)

#### Prerequisites Build Steps

Before deploying, ensure your code is ready:

1. **Compile Backend (Optional - Docker handles this)**:
   ```cmd
   cd backend
   mvn clean package -DskipTests
   cd ..
   ```
   *Note: The Docker build process includes compilation, so this step is optional but useful for local testing.*

2. **Build Frontend (Optional - for production builds)**:
   ```cmd
   cd frontend
   npm install
   npm run build
   cd ..
   ```
   *Note: For development, the frontend runs separately. For production deployment, build first.*

#### Deployment Steps

1. **Build and Start Multi-Pod Environment**:
   ```cmd
   npm run multi-pod:start
   ```
   
   This script automatically:
   - ✅ Checks Docker prerequisites
   - ✅ Builds the backend Docker image (includes Maven compilation)
   - ✅ Stops any existing containers
   - ✅ Starts multi-pod environment with PostgreSQL, Redis, and 2 backend pods
   - ✅ Sets up Nginx load balancer

2. **Test the Deployment**:
   ```cmd
   npm run multi-pod:test
   ```

3. **Monitor Redis Pub/Sub**:
   ```cmd
   npm run multi-pod:redis-monitor
   ```

4. **View Logs**:
   ```cmd
   npm run multi-pod:logs
   ```

5. **Stop Environment**:
   ```cmd
   npm run multi-pod:stop
   ```

### Manual Deployment (For Custom Builds)

If you prefer manual control over the build and deployment process:

1. **Compile Backend**:
   ```cmd
   cd backend
   mvn clean package -DskipTests
   cd ..
   ```

2. **Build Docker Image**:
   ```cmd
   cd backend
   docker build -t taskapp/backend:latest .
   cd ..
   ```

3. **Build Frontend (Optional)**:
   ```cmd
   cd frontend
   npm install
   npm run build
   cd ..
   ```

4. **Start Infrastructure Services**:
   ```cmd
   cd docker
   docker-compose -f docker-compose.multi-pod.yml up -d postgres redis nginx
   ```

5. **Start Backend Pods**:
   ```cmd
   docker-compose -f docker-compose.multi-pod.yml up -d backend-1 backend-2
   cd ..
   ```

6. **Verify Deployment**:
   ```cmd
   cd docker
   docker-compose -f docker-compose.multi-pod.yml ps
   cd ..
   ```

## Available Endpoints

- **Load Balanced (Nginx)**: http://localhost:8090
- **Backend Pod 1 Direct**: http://localhost:8082
- **Backend Pod 2 Direct**: http://localhost:8083
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379
- **Frontend (when running locally)**: http://localhost:5173

## Testing Cross-Pod SSE Functionality

### Manual Testing

1. **Open Multiple Browser Tabs**:
   - Tab 1: Connect to Pod 1 (http://localhost:8082)
   - Tab 2: Connect to Pod 2 (http://localhost:8083)
   - Tab 3: Load Balancer (http://localhost:8090)

2. **Login with Different Users**:
   - User 1 in Tab 1
   - User 2 in Tab 2
   - User 3 in Tab 3

3. **Create/Modify Tasks**:
   - Create a task in Tab 1 assigned to User 2
   - Observe real-time notification delivery to Tab 2 (cross-pod)
   - Check Redis pub/sub logs for message flow

### Automated Testing

Run the automated test suite:
```cmd
npm run multi-pod:test
```

### Monitoring

1. **Application Logs**:
   ```cmd
   npm run multi-pod:logs
   
   # Or directly with Docker Compose
   cd docker
   docker-compose -f docker-compose.multi-pod.yml logs -f backend-1
   docker-compose -f docker-compose.multi-pod.yml logs -f backend-2
   ```

2. **Redis Pub/Sub Messages**:
   ```cmd
   npm run multi-pod:redis-monitor
   
   # Or directly
   docker exec todo_redis_multi redis-cli monitor
   ```

3. **Health Checks**:
   ```cmd
   # Pod 1
   curl http://localhost:8082/actuator/health
   
   # Pod 2
   curl http://localhost:8083/actuator/health
   
   # Load Balancer
   curl http://localhost:8090/actuator/health
   ```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `POD_ID` | Unique identifier for pod instance | Generated from pod name |
| `SPRING_PROFILES_ACTIVE` | Spring Boot profile | `dev` or `prod` |
| `MESSAGING_PROVIDER` | Messaging provider (currently `redis`) | `redis` |
| `SPRING_DATA_REDIS_HOST` | Redis host | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |

### Redis Configuration

The Redis instance is configured for pub/sub messaging with:
- Persistence enabled for reliability
- Connection pooling for performance
- Health checks for monitoring

### Load Balancer Configuration

Nginx is configured with:
- Round-robin load balancing
- Sticky sessions for SSE connections (IP hash)
- Long timeouts for SSE streams
- Health check endpoints

## Troubleshooting

### Common Issues

1. **Build Failures**:
   ```cmd
   # Check Java version (requires Java 17+)
   java -version
   
   # Check Maven version
   mvn -version
   
   # Clean and rebuild
   cd backend
   mvn clean package -DskipTests
   cd ..
   
   # If Docker build fails
   docker system prune -f
   cd backend
   docker build --no-cache -t taskapp/backend:latest .
   cd ..
   ```

2. **Containers Not Starting**:
   ```cmd
   # Check Docker status
   docker ps
   cd docker
   docker-compose -f docker-compose.multi-pod.yml ps
   
   # Check logs
   docker-compose -f docker-compose.multi-pod.yml logs
   
   # Restart with clean slate
   docker-compose -f docker-compose.multi-pod.yml down -v
   cd ..
   npm run multi-pod:start
   ```

3. **SSE Connections Not Working**:
   - Verify JWT tokens are valid
   - Check CORS configuration
   - Ensure Redis is running and accessible
   - Check load balancer configuration

4. **Cross-Pod Messages Not Delivered**:
   - Verify Redis pub/sub is working: `docker exec todo_redis_multi redis-cli monitor`
   - Check pod IDs are unique (MESSAGING_POD_ID environment variable)
   - Verify message consumer is running on both pods

5. **Database Connection Issues**:
   ```cmd
   # Check PostgreSQL status
   docker exec todo_postgres_multi pg_isready -U todo_user -d todo_db
   
   # Check database logs
   docker logs todo_postgres_multi
   ```

### Performance Tuning

1. **Redis Connection Pool**:
   - Adjust `spring.data.redis.lettuce.pool.max-active` for higher load
   - Monitor connection usage with Redis INFO command

2. **JVM Memory**:
   - Set `JAVA_OPTS` for heap size: `-Xmx1g -Xms512m`
   - Monitor memory usage with actuator metrics

3. **Database Connections**:
   - Tune HikariCP pool settings in application properties
   - Monitor connection pool metrics

## Security Considerations

1. **JWT Secrets**:
   - Use strong, unique secrets in production
   - Rotate secrets regularly
   - Store in Docker secrets or environment variables

2. **Database Credentials**:
   - Use strong passwords
   - Store in secrets management system
   - Enable SSL/TLS for database connections

3. **Redis Security**:
   - Enable authentication in production
   - Use Redis ACLs for fine-grained access control
   - Consider Redis TLS for network encryption

## Scaling

### Horizontal Scaling

1. **Add More Pods**:
   - Update `docker-compose.multi-pod.yml` to add backend-3, backend-4, etc.
   - Update Nginx configuration to include new backend servers
   - Ensure each pod has a unique MESSAGING_POD_ID

2. **Example: Adding Backend Pod 3**:
   ```yaml
   # Add to docker-compose.multi-pod.yml
   backend-3:
     image: todo-backend:latest
     container_name: todo_backend_3
     environment:
       # ...same as other pods...
       - MESSAGING_POD_ID=backend-3
     ports:
       - "8084:8080"
   ```

### Database Scaling

1. **Read Replicas**:
   - Configure read-only database connections
   - Use Spring's `@Transactional(readOnly = true)` for queries

2. **Connection Pooling**:
   - Increase database connection pool size
   - Monitor connection utilization

## Monitoring and Observability

### Metrics to Monitor

- SSE connection counts per pod
- Message delivery success/failure rates
- Redis pub/sub message throughput
- Database connection pool utilization
- Load balancer response times

### Logging

- Structured logging with correlation IDs
- Message flow tracing across pods
- Performance metrics logging
- Error aggregation and analysis

### Health Checks

Each component includes health checks:
- Spring Boot Actuator endpoints
- Redis ping checks
- PostgreSQL connection checks
- Nginx upstream health monitoring

This multi-pod deployment provides a production-ready foundation for horizontal scaling while maintaining real-time notification capabilities across all instances.
