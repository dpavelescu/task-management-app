import { useState, useEffect, useCallback } from 'react';
import { useAuth } from './useAuth';
import { getTasks, createTask, deleteTask } from '../api/tasks';
import type { Task } from '../types/api';

export function useSimpleTaskManager() {
  const { token, isAuthenticated } = useAuth();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);  // Simple function to fetch tasks
  const fetchTasks = useCallback(async () => {
    if (!isAuthenticated || !token) return;
    
    try {
      setLoading(true);
      setError(null);
      console.log('TaskManager: Fetching tasks...');
      const fetchedTasks = await getTasks();
      console.log('TaskManager: Fetched tasks:', fetchedTasks.length, 'tasks:', fetchedTasks.map(t => `${t.id}:${t.title}`));
      setTasks(fetchedTasks);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to fetch tasks';
      console.error('Error fetching tasks:', errorMessage);
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated, token]);
  // Create task function
  const addTask = useCallback(async (taskData: { title: string; description: string; assignedTo?: number }) => {
    try {
      setError(null);
      const newTask = await createTask(taskData);
      // Add the new task to the state immediately
      setTasks(prev => [...prev, newTask]);
      return newTask;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to create task';
      console.error('Error creating task:', errorMessage);
      setError(errorMessage);
      throw err;
    }
  }, []);
  // Delete task function
  const removeTask = useCallback(async (taskId: number) => {
    try {
      setError(null);
      await deleteTask(taskId);
      // Remove the task from state immediately
      setTasks(prev => prev.filter(task => task.id !== taskId));
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to delete task';
      console.error('Error deleting task:', errorMessage);
      setError(errorMessage);
      throw err;
    }
  }, []);  // Refresh tasks (called by SSE notifications)
  const refreshTasks = useCallback(() => {
    console.log('TaskManager: refreshTasks called by SSE notification');
    fetchTasks();
  }, [fetchTasks]);

  // Initial load
  useEffect(() => {
    if (isAuthenticated) {
      fetchTasks();
    } else {
      setTasks([]);
      setError(null);
    }
  }, [isAuthenticated, fetchTasks]);

  return {
    tasks,
    loading,
    error,
    addTask,
    removeTask,
    refreshTasks,
    refetch: fetchTasks
  };
}
