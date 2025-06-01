import { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Box,
  Button,
  TextField,
  Typography,
  Paper,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { getUsers } from '../api/users';
import { useAuth } from '../hooks/useAuth';
import { useSimpleTaskManager } from '../hooks/useSimpleTaskManager';
import { useSimpleSSE } from '../hooks/useSimpleSSE';
import { useDebounceInput } from '../hooks/useDebounceInput';
import { useErrorNotification } from '../components/useErrorNotification';
import TaskList from '../components/TaskList';
import type { User } from '../types/api';

// Export Tasks component as named export to match App.tsx import
export function Tasks() {
  const navigate = useNavigate();
  const { logout, isAuthenticated, isInitialized, user } = useAuth();
  const { showError, showSuccess } = useErrorNotification();
  
  // Use debounced inputs for better performance
  const titleInput = useDebounceInput();
  const descriptionInput = useDebounceInput();
  const [assignedToId, setAssignedToId] = useState<number | ''>('');
  const [users, setUsers] = useState<User[]>([]);
  const [loadingUsers, setLoadingUsers] = useState(false);
  
  // Use simplified task manager
  const { tasks, loading, error, addTask, removeTask, refreshTasks } = useSimpleTaskManager();
    // Use simplified SSE
  const { isConnected } = useSimpleSSE({ onTaskUpdate: refreshTasks });

  // Memoize expensive filtering operations to prevent re-computation on every render
  const createdTasks = useMemo(() => 
    tasks?.filter(task => task.createdByUsername === user?.username) || [], 
    [tasks, user?.username]
  );
  
  const assignedTasks = useMemo(() => 
    tasks?.filter(task => task.assignedToUsername === user?.username) || [], 
    [tasks, user?.username]
  );

  // Check authentication on mount and when auth state changes
  useEffect(() => {
    // Wait for auth to initialize before checking
    if (!isInitialized) {
      return;
    }
    
    if (!isAuthenticated) {
      navigate('/login');
    }
  }, [isAuthenticated, isInitialized, navigate]);
  
  // Load users for assignment dropdown
  useEffect(() => {
    const loadUsers = async () => {
      if (!isAuthenticated) return;
      
      try {
        setLoadingUsers(true);
        const fetchedUsers = await getUsers();
        setUsers(fetchedUsers);
      } catch (err) {
        console.error('Error loading users:', err);
        const errorMessage = err instanceof Error ? err.message : 'Failed to load users';
        if (errorMessage.includes('401') || errorMessage.includes('403')) {
          logout();
          navigate('/login');
        } else {
          showError(`Failed to load users: ${errorMessage}`);
        }
      } finally {
        setLoadingUsers(false);
      }
    };

    loadUsers();
  }, [isAuthenticated, logout, navigate, showError]);
  // Handle query errors
  useEffect(() => {
    if (error) {
      if (error.includes('Session expired') || 
          error.includes('Not authenticated') ||
          error.includes('Authentication required') ||
          error.includes('401') || 
          error.includes('403')) {
        logout();
        navigate('/login');
      } else {
        // Show error notification for non-auth errors
        showError(`Failed to load tasks: ${error}`);
      }
    }
  }, [error, logout, navigate, showError]);
  // Memoize handlers to prevent unnecessary re-renders
  const handleAddTask = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await addTask({
        title: titleInput.value,
        description: descriptionInput.value,
        assignedTo: assignedToId || undefined,
      });
      titleInput.reset();
      descriptionInput.reset();
      setAssignedToId('');
      showSuccess('Task created successfully!');
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to create task';
      showError(errorMessage);
    }
  }, [titleInput, descriptionInput, assignedToId, addTask, showSuccess, showError]);
  
  const handleDeleteTask = useCallback(async (taskId: number) => {
    if (window.confirm('Are you sure you want to delete this task?')) {
      try {
        await removeTask(taskId);
        showSuccess('Task deleted successfully!');
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to delete task';
        showError(errorMessage);
      }
    }
  }, [removeTask, showSuccess, showError]);

  const handleAssigneeChange = useCallback((event: { target: { value: unknown } }) => {
    setAssignedToId(event.target.value as number | '');
  }, []);

  // Direct handling of task deletion is now provided to TaskList component
    
  // Show loading while auth is initializing
  if (!isInitialized) {
    return (
      <Box display="flex" justifyContent="center" mt={4}>
        <CircularProgress />
        <Typography sx={{ ml: 2 }}>Loading...</Typography>
      </Box>
    );
  }
    if (loading) {
    return (
      <Box display="flex" justifyContent="center" mt={4}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto', p: 3 }}>
      {/* SSE connection status indicator */}
      <Box sx={{ mb: 2, display: 'flex', justifyContent: 'flex-end' }}>
        <Typography 
          variant="caption" 
          sx={{ 
            color: isConnected ? 'success.main' : 'warning.main',
            fontWeight: 'medium'
          }}
        >
          {isConnected ? '🟢 SSE notifications active' : '🟡 Connecting to notifications...'}
        </Typography>
      </Box>
      
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          Add New Task
        </Typography>        <Box component="form" onSubmit={handleAddTask}>          <TextField
            fullWidth
            label="Title"
            value={titleInput.value}
            onChange={titleInput.handleChange}
            margin="normal"
            required
            error={loading}
            disabled={loading}
          />
          <TextField
            fullWidth
            label="Description"
            value={descriptionInput.value}
            onChange={descriptionInput.handleChange}
            margin="normal"
            multiline
            rows={3}
            error={loading}
            disabled={loading}
          />
          <FormControl fullWidth margin="normal">
            <InputLabel id="assignee-select-label">Assign to (optional)</InputLabel>            <Select
              labelId="assignee-select-label"
              value={assignedToId}
              label="Assign to (optional)"
              onChange={handleAssigneeChange}
              disabled={loading || loadingUsers}
            >
              <MenuItem value="">
                <em>No assignment</em>
              </MenuItem>
              {users.map((user) => (
                <MenuItem key={user.id} value={user.id}>
                  {user.username} ({user.email})
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button
            type="submit"
            variant="contained"
            color="primary"
            sx={{ mt: 2 }}
            disabled={loading}
          >
            {loading ? 'Adding...' : 'Add Task'}
          </Button>
        </Box>
      </Paper>      <Paper sx={{ p: 3 }}>        <Typography variant="h6" gutterBottom>
          Tasks Created by You
        </Typography>
        <TaskList 
          tasks={createdTasks} 
          emptyMessage="No tasks created by you" 
          showDeleteButtons={true}
          onDeleteTask={handleDeleteTask}
        />

        <Typography variant="h6" gutterBottom sx={{ mt: 4 }}>
          Tasks Assigned to You
        </Typography>
        <TaskList 
          tasks={assignedTasks} 
          emptyMessage="No tasks assigned to you" 
        />
      </Paper>
    </Box>
  );
}

// Also export as default for backward compatibility
export default Tasks;
