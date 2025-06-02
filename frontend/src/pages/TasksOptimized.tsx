import { useState, useEffect, useCallback, useRef } from 'react';
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
import type { SelectChangeEvent } from '@mui/material';
import { Delete as DeleteIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useSimpleSSE } from '../hooks/useSimpleSSE';
import { useErrorNotification } from '../components/useErrorNotification';
import { getTasks, createTask, deleteTask } from '../api/tasks';
import { getUsers } from '../api/users';
import type { User, Task } from '../types/api';

// Memoized TaskList component
const TaskList = ({ 
  tasks, 
  title, 
  onDelete, 
  emptyMessage 
}: {
  tasks: Task[];
  title: string;
  onDelete: (taskId: number) => void;
  emptyMessage: string;
}) => (
  <Paper sx={{ p: 3, mb: 3 }}>
    <Typography variant="h6" gutterBottom>
      {title}
    </Typography>
    <List>
      {tasks.length > 0 ? (
        tasks.map(task => (
          <ListItem key={task.id}>
            <ListItemText
              primary={task.title}
              secondary={`${task.description} | Created by: ${task.createdByUsername} | Assigned to: ${task.assignedToUsername || 'Unassigned'}`}
            />
            <ListItemSecondaryAction>
              <IconButton edge="end" onClick={() => onDelete(task.id)}>
                <DeleteIcon />
              </IconButton>
            </ListItemSecondaryAction>
          </ListItem>
        ))
      ) : (
        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 2 }}>
          {emptyMessage}
        </Typography>
      )}
    </List>
  </Paper>
);

export function Tasks() {
  const navigate = useNavigate();
  const { logout, isAuthenticated, isInitialized, user } = useAuth();
  const { showError, showSuccess } = useErrorNotification();
  
  // Form state using refs for uncontrolled components
  const titleRef = useRef<HTMLInputElement>(null);
  const descriptionRef = useRef<HTMLInputElement>(null);
  const [selectedAssignee, setSelectedAssignee] = useState<number | ''>('');
  
  // Task state
  const [createdTasks, setCreatedTasks] = useState<Task[]>([]);
  const [assignedTasks, setAssignedTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  
  // User state
  const [users, setUsers] = useState<User[]>([]);
  const [usersLoading, setUsersLoading] = useState(false);
    // Prevent concurrent API calls
  const operationInProgress = useRef(false);

  // Load tasks
  const loadTasks = useCallback(async () => {
    if (!user || operationInProgress.current) return;
    
    operationInProgress.current = true;
    setLoading(true);
    
    try {
      const allTasks = await getTasks();
        // Split tasks in one operation
      const created = allTasks.filter(task => task.createdByUsername === user.username);
      const assigned = allTasks.filter(task => task.assignedToUsername === user.username);
      
      setCreatedTasks(created);
      setAssignedTasks(assigned);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to load tasks';
      console.error('Error loading tasks:', error);
      showError(message);
    } finally {
      setLoading(false);
      operationInProgress.current = false;
    }
  }, [user, showError]);

  // Load users
  const loadUsers = useCallback(async () => {
    if (operationInProgress.current) return;
    
    setUsersLoading(true);
    
    try {
      const fetchedUsers = await getUsers();
      setUsers(fetchedUsers);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to load users';
      console.error('Error loading users:', error);
      showError(message);
    } finally {
      setUsersLoading(false);    }
  }, [showError]);

  // Handle auth changes
  useEffect(() => {
    if (!isInitialized) return;
    
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    
    // Load initial data
    loadTasks();
    loadUsers();
  }, [isInitialized, isAuthenticated, navigate, loadTasks, loadUsers]);

  // Handle task creation
  const handleSubmit = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!titleRef.current || !descriptionRef.current || !user) return;
    
    const title = titleRef.current.value.trim();
    const description = descriptionRef.current.value.trim();
    
    if (!title || !description) {
      showError('Please fill in all required fields');
      return;
    }
    
    setSubmitting(true);
    
    try {
      const newTask = await createTask({
        title,
        description,
        assignedTo: selectedAssignee || undefined,
      });
        // Optimistic update
      if (newTask.createdByUsername === user.username) {
        setCreatedTasks(prev => [newTask, ...prev]);
      }
      if (newTask.assignedToUsername === user.username) {
        setAssignedTasks(prev => [newTask, ...prev]);
      }
      
      // Clear form
      titleRef.current.value = '';
      descriptionRef.current.value = '';
      setSelectedAssignee('');
      
      showSuccess('Task created successfully');
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to create task';
      console.error('Error creating task:', error);
      showError(message);
    } finally {
      setSubmitting(false);
    }
  }, [selectedAssignee, user, showError, showSuccess]);

  // Handle task deletion
  const handleDeleteTask = useCallback(async (taskId: number) => {
    try {
      await deleteTask(taskId);
      
      // Optimistic update
      setCreatedTasks(prev => prev.filter(task => task.id !== taskId));
      setAssignedTasks(prev => prev.filter(task => task.id !== taskId));
      
      showSuccess('Task deleted successfully');
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to delete task';
      console.error('Error deleting task:', error);
      showError(message);
    }  }, [showError, showSuccess]);
  
  // Handle SSE updates  
  const handleTaskUpdate = useCallback(() => {
    console.log('SSE notification received - refreshing tasks');
    // Debounce refresh to avoid excessive API calls
    setTimeout(loadTasks, 100);
  }, [loadTasks]);

  // Setup SSE connection
  const { isConnected } = useSimpleSSE({ onTaskUpdate: handleTaskUpdate });

  if (!isInitialized) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto', p: 3 }}>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" component="h1">
          Task Manager
        </Typography>
        <Box display="flex" alignItems="center" gap={2}>
          <Typography variant="body2" color={isConnected ? 'success.main' : 'error.main'}>
            {isConnected ? '🟢 Connected' : '🔴 Disconnected'}
          </Typography>
          <Button variant="outlined" onClick={logout}>
            Logout
          </Button>
        </Box>
      </Box>

      {/* Task Creation Form */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          Create New Task
        </Typography>
        <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            inputRef={titleRef}
            label="Task Title"
            required
            fullWidth
            disabled={submitting}
          />
          <TextField
            inputRef={descriptionRef}
            label="Description"
            required
            multiline
            rows={3}
            fullWidth
            disabled={submitting}
          />
          <FormControl fullWidth disabled={submitting || usersLoading}>
            <InputLabel>Assign To (Optional)</InputLabel>
            <Select
              value={selectedAssignee}
              onChange={(e: SelectChangeEvent<number | ''>) => setSelectedAssignee(e.target.value)}
              label="Assign To (Optional)"
            >
              <MenuItem value="">
                <em>Unassigned</em>
              </MenuItem>
              {users.map(user => (
                <MenuItem key={user.id} value={user.id}>
                  {user.username} ({user.email})
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button 
            type="submit" 
            variant="contained" 
            disabled={submitting || loading}
            sx={{ alignSelf: 'flex-start' }}
          >
            {submitting ? 'Creating...' : 'Create Task'}
          </Button>
        </Box>
      </Paper>

      {/* Loading State */}
      {loading && (
        <Box display="flex" justifyContent="center" my={4}>
          <CircularProgress />
        </Box>
      )}

      {/* Task Lists */}
      {!loading && (
        <>
          <TaskList
            tasks={createdTasks}
            title="Tasks Created by You"
            onDelete={handleDeleteTask}
            emptyMessage="You haven't created any tasks yet."
          />
          
          <TaskList
            tasks={assignedTasks}
            title="Tasks Assigned to You"
            onDelete={handleDeleteTask}
            emptyMessage="No tasks have been assigned to you."
          />
        </>
      )}
    </Box>
  );
}
