import { useState, useEffect } from 'react';
import {
  Box,
  Button,
  TextField,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Typography,
  Paper,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { Delete as DeleteIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { getUsers } from '../api/users';
import { useAuth } from '../hooks/useAuth';
import { useSimpleTaskManager } from '../hooks/useSimpleTaskManager';
import { useSimpleSSE } from '../hooks/useSimpleSSE';
import { useErrorNotification } from '../components/useErrorNotification';
import type { User, Task } from '../types/api';

// Export Tasks component as named export to match App.tsx import
export function Tasks() {
  const navigate = useNavigate();
  const { logout, isAuthenticated, isInitialized, user } = useAuth();
  const { showError, showSuccess } = useErrorNotification();
  
  // Simple state - no debouncing needed for low-volume usage
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [assignedToId, setAssignedToId] = useState<number | ''>('');  const [users, setUsers] = useState<User[]>([]);
  const [loadingUsers, setLoadingUsers] = useState(false);
    // Pre-computed filtered tasks to avoid expensive filtering on every render
  const [createdTasks, setCreatedTasks] = useState<Task[]>([]);
  const [assignedTasks, setAssignedTasks] = useState<Task[]>([]);// Use simplified task manager
  const { tasks, loading, error, addTask, removeTask, refreshTasks } = useSimpleTaskManager();
    // Use simplified SSE
  const { isConnected } = useSimpleSSE({ onTaskUpdate: refreshTasks });
  // Combined effect to reduce re-renders - only trigger when auth actually changes
  useEffect(() => {
    // Wait for auth to initialize before checking
    if (!isInitialized) return;
    
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    // Load users once when authenticated
    const loadUsers = async () => {      
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
  }, [isAuthenticated, isInitialized, navigate, logout, showError]);
  
  // Separate error handler - only when error actually changes
  useEffect(() => {
    if (!error) return;
    
    if (error.includes('Session expired') || 
        error.includes('Not authenticated') ||
        error.includes('Authentication required') ||
        error.includes('401') || 
        error.includes('403')) {
      logout();
      navigate('/login');
    } else {
      // Show error notification for non-auth errors      showError(`Failed to load tasks: ${error}`);
    }
  }, [error, logout, navigate, showError]);

  // Pre-compute filtered tasks when tasks or user changes
  useEffect(() => {
    if (tasks && user) {
      setCreatedTasks(tasks.filter(task => task.createdByUsername === user.username));
      setAssignedTasks(tasks.filter(task => task.assignedToUsername === user.username));
    } else {
      setCreatedTasks([]);
      setAssignedTasks([]);
    }
  }, [tasks, user]);

  const handleAddTask = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await addTask({
        title,
        description,
        assignedTo: assignedToId || undefined,
      });
      setTitle('');
      setDescription('');
      setAssignedToId('');
      showSuccess('Task created successfully!');
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to create task';
      showError(errorMessage);
    }
  };
  
  const handleDeleteTask = async (taskId: number) => {
    if (window.confirm('Are you sure you want to delete this task?')) {
      try {
        await removeTask(taskId);
        showSuccess('Task deleted successfully!');
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to delete task';
        showError(errorMessage);
      }
    }
  };
    
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
        </Typography>        <Box component="form" onSubmit={handleAddTask}>
          <TextField
            fullWidth
            label="Title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            margin="normal"
            required
            disabled={loading}
          />
          <TextField
            fullWidth
            label="Description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            margin="normal"
            multiline
            rows={3}
            disabled={loading}
          />
          <FormControl fullWidth margin="normal">
            <InputLabel id="assignee-select-label">Assign to (optional)</InputLabel>
            <Select
              labelId="assignee-select-label"
              value={assignedToId}
              label="Assign to (optional)"
              onChange={(e) => setAssignedToId(e.target.value as number | '')}
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
        </Box>      </Paper>

      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Tasks Created by You
        </Typography>        <List>
          {createdTasks.length > 0 ? (
            createdTasks.map(task => (
              <ListItem key={task.id}>
                <ListItemText 
                  primary={task.title} 
                  secondary={
                    <Box>
                      <Typography variant="body2" component="span">{task.description}</Typography>
                      {task.assignedToUsername && (
                        <Typography variant="caption" color="text.secondary" component="span" sx={{ display: 'block' }}>
                          Assigned to: {task.assignedToUsername}
                        </Typography>
                      )}
                    </Box>
                  }
                  secondaryTypographyProps={{ component: 'div' }}
                />
                <ListItemSecondaryAction>
                  <IconButton
                    edge="end"
                    aria-label="delete"
                    onClick={() => handleDeleteTask(task.id)}
                    color="error"
                  >
                    <DeleteIcon />
                  </IconButton>
                </ListItemSecondaryAction>
              </ListItem>
            ))
          ) : (
            <ListItem>
              <ListItemText primary="No tasks created by you" />
            </ListItem>
          )}
        </List>        <Typography variant="h6" gutterBottom sx={{ mt: 4 }}>
          Tasks Assigned to You
        </Typography>
        <List>
          {assignedTasks.length > 0 ? (
            assignedTasks.map(task => (
              <ListItem key={task.id}>
                <ListItemText 
                  primary={task.title} 
                  secondary={
                    <Box>
                      <Typography variant="body2" component="span">{task.description}</Typography>
                      <Typography variant="caption" color="text.secondary" component="span" sx={{ display: 'block' }}>
                        Created by: {task.createdByUsername}
                      </Typography>
                    </Box>
                  }
                  secondaryTypographyProps={{ component: 'div' }}
                />
              </ListItem>
            ))
          ) : (
            <ListItem>
              <ListItemText primary="No tasks assigned to you" />
            </ListItem>
          )}
        </List>
      </Paper>
    </Box>
  );
}

// Also export as default for backward compatibility
export default Tasks;
