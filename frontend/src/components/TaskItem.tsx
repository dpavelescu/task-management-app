import { memo } from 'react';
import {
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Typography,
  Box
} from '@mui/material';
import { Delete as DeleteIcon } from '@mui/icons-material';
import type { Task } from '../types/api';

interface TaskItemProps {
  task: Task;
  showDeleteButton?: boolean;
  onDelete?: (taskId: number) => void;
}

// Memoized TaskItem to prevent re-rendering when other tasks change
const TaskItem = memo(function TaskItem({ task, showDeleteButton = false, onDelete }: TaskItemProps) {
  return (
    <ListItem>
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
            {task.createdByUsername && !showDeleteButton && (
              <Typography variant="caption" color="text.secondary" component="span" sx={{ display: 'block' }}>
                Created by: {task.createdByUsername}
              </Typography>
            )}
          </Box>
        }
        secondaryTypographyProps={{ component: 'div' }}
      />
      {showDeleteButton && onDelete && (
        <ListItemSecondaryAction>
          <IconButton
            edge="end"
            aria-label="delete"
            onClick={() => onDelete(task.id)}
            color="error"
          >
            <DeleteIcon />
          </IconButton>
        </ListItemSecondaryAction>
      )}
    </ListItem>
  );
});

export default TaskItem;
