# Frontend - React Task Management UI

This is the frontend component of the Real-Time Task Management Application, built with React 18+ and TypeScript.

## Overview

- **Framework**: React 18+ with TypeScript
- **Build Tool**: Vite for fast development and optimized builds
- **Styling**: Modern CSS with responsive design
- **State Management**: React Context API + Custom Hooks
- **Real-time**: EventSource for Server-Sent Events (SSE)

## Project Structure

```
src/
├── api/             # HTTP client functions
│   ├── auth.ts      # Authentication API calls
│   ├── tasks.ts     # Task management API calls
│   └── users.ts     # User API calls
├── components/      # Reusable UI components
│   ├── ErrorBoundary.tsx
│   ├── ErrorNotificationProvider.tsx
│   └── Layout.tsx
├── config/          # Configuration management
│   └── index.ts     # Environment-based configuration
├── contexts/        # React contexts
│   └── EnhancedAuthProvider.tsx
├── hooks/           # Custom React hooks
│   ├── useAuth.ts
│   ├── useSimpleSSE.ts
│   └── useSimpleTaskManager.ts
├── pages/           # Page components
│   ├── LoginPage.tsx
│   ├── Register.tsx
│   └── Tasks.tsx
├── types/           # TypeScript interfaces
│   └── api.ts
└── utils/           # Helper functions
    └── jwtUtils.ts
```

## Key Features

### Authentication
- JWT-based authentication with automatic token refresh
- Protected routes and authenticated API calls
- Secure token storage and management

### Task Management
- Create, edit, and delete tasks
- Assign tasks to other users
- Real-time status updates
- Priority and status management

### Real-Time Updates
- Server-Sent Events (SSE) for instant notifications
- Automatic data refresh on notifications
- Cross-tab synchronization
- Connection management and error handling

### User Experience
- Responsive design for mobile and desktop
- Loading states and error handling
- Toast notifications for user feedback
- Clean, modern interface

## Configuration

### Environment Variables
Create environment files in the frontend directory:

#### `.env.development`
```env
VITE_API_URL=http://localhost:8080/api
VITE_SSE_URL=http://localhost:8080/api/notifications/stream
VITE_ENVIRONMENT=development
```

#### `.env.production`
```env
VITE_API_URL=https://your-api-domain.com/api
VITE_SSE_URL=https://your-api-domain.com/api/notifications/stream
VITE_ENVIRONMENT=production
```

### Build Configuration
The Vite configuration includes:
- TypeScript support
- Development proxy to backend API
- Optimized production builds
- Hot Module Replacement (HMR)

## Development

### Setup
```cmd
cd frontend
npm install
```

### Running
```cmd
# Development server with HMR
npm run dev

# Preview production build
npm run preview

# Development server on specific port
npm run dev -- --port 3000
```

### Building
```cmd
# Production build (outputs to dist/)
npm run build

# Type checking only
npm run type-check

# Linting
npm run lint

# Clean build (remove dist/ first)
rmdir /s /q dist && npm run build
```

#### Build Outputs
- **Development**: Served directly from `src/` with Vite dev server
- **Production**: Static files in `dist/` directory ready for deployment
- **Build size**: Optimized bundle with tree-shaking and minification

## API Integration

### Authentication Flow
1. User logs in → JWT tokens stored securely
2. API calls include Authorization header
3. Automatic token refresh on expiration
4. Logout clears all stored tokens

### Real-Time Connection
1. SSE connection established on login
2. Listens for task-related notifications
3. Automatically refetches data on updates
4. Handles connection errors and reconnection

### Task Operations
- All CRUD operations with optimistic UI updates
- Real-time synchronization across browser tabs
- Error handling with user-friendly messages
- Loading states for better UX

## Components Overview

### Core Components
- **Layout**: Main application shell with navigation
- **ErrorBoundary**: Catches and displays React errors
- **ErrorNotificationProvider**: Global error handling

### Custom Hooks
- **useAuth**: Authentication state and operations
- **useSimpleSSE**: Server-Sent Events management
- **useSimpleTaskManager**: Task CRUD operations

### Pages
- **LoginPage**: Authentication interface
- **Register**: User registration
- **Tasks**: Main task management interface

## TypeScript Integration

The application is fully typed with:
- API response interfaces
- Component prop types
- Custom hook return types
- Event handler types

## Responsive Design

- Mobile-first approach
- Flexible grid layouts
- Touch-friendly interactions
- Progressive enhancement

## Testing

```bash
# Run tests (when implemented)
npm run test

# Test coverage
npm run test:coverage
```

## Deployment

### Development
```bash
npm run dev
```

### Production
```bash
# Build for production
npm run build

# Serve built files
npm run preview
```

The built files will be in the `dist/` directory, ready for deployment to any static hosting service.

## Performance

- Code splitting with dynamic imports
- Optimized bundle size with Vite
- Efficient re-renders with React hooks
- Cached API responses where appropriate
  },
})
```
