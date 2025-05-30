# API Testing with REST Client

This directory contains modern HTTP files for testing the Task Management API using VS Code's REST Client extension.

## Setup

1. **Install VS Code Extension**: 
   - Install "REST Client" extension by Huachao Mao
   - VS Code will prompt to install recommended extensions

2. **Start the Application**:
   ```bash
   npm run dev
   ```

## Testing Files

### 📄 `api-tests.http`
Complete API testing suite with authentication flow:
- User registration and login
- Token-based authentication
- Task CRUD operations
- User management

**Usage**: Open file in VS Code, click "Send Request" above each HTTP request.

### 📄 `sse-tests.http` 
Server-Sent Events testing:
- SSE connection endpoints
- Real-time notification testing
- Instructions for multi-user testing

### 📄 `status-checks.http`
System health checks:
- Backend health endpoints
- Service availability checks
- Quick status verification

## Quick Start Testing

1. **Basic Flow**:
   ```
   1. Open api-tests.http
   2. Run "User Registration" requests
   3. Run "Login" request 
   4. Copy the accessToken from response
   5. Run other requests with authentication
   ```

2. **Real-time Testing**:
   ```
   1. Start frontend: npm run dev:frontend
   2. Login two users in different browser tabs
   3. Create/update tasks and observe real-time updates
   ```

## Development Scripts

Use these npm scripts from the root directory:

```bash
# Start both backend and frontend
npm run dev

# Start only backend (Docker)
npm run dev:backend

# Start only frontend
npm run dev:frontend

# View backend logs
npm run dev:logs

# Check service status
npm run status

# Stop all services
npm run stop
```

## Advantages over .bat files

✅ **Cross-platform**: Works on Windows, Mac, Linux  
✅ **VS Code integrated**: Native syntax highlighting and execution  
✅ **Professional**: Industry standard approach  
✅ **Readable**: Clean HTTP syntax  
✅ **Maintainable**: Easy to update and version control  
✅ **Shareable**: Team members can use the same files  

## Tips

- Use `@name` comments to reference previous requests
- Variables (starting with @) can be reused across requests
- Check the Output panel for detailed response information
- Use Ctrl+Shift+P → "Rest Client: Send Request" for keyboard shortcuts
