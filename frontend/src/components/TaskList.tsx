import { memo } from 'react';
import { List, ListItem, ListItemText } from '@mui/material';
import TaskItem from './TaskItem';
import type { Task } from '../types/api';

interface TaskListProps {
  tasks: Task[];
  emptyMessage: string;
  showDeleteButtons?: boolean;
  onDeleteTask?: (taskId: number) => void;
}

// Memoized TaskList to prevent re-rendering when parent component re-renders
const TaskList = memo(function TaskList({ 
  tasks, 
  emptyMessage, 
  showDeleteButtons = false,
  onDeleteTask
}: TaskListProps) {
  if (tasks.length === 0) {
    return (
      <List>
        <ListItem>
          <ListItemText primary={emptyMessage} />
        </ListItem>
      </List>
    );
  }
  
  return (
    <List>
      {tasks.map(task => (
        <TaskItem 
          key={task.id} 
          task={task} 
          showDeleteButton={showDeleteButtons}
          onDelete={onDeleteTask}
        />
      ))}
    </List>
  );
});

export default TaskList;
