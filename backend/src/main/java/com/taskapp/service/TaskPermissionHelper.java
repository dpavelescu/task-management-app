package com.taskapp.service;

import com.taskapp.entity.Task;
import com.taskapp.entity.User;
import com.taskapp.exception.TaskPermissionException;
import org.springframework.stereotype.Component;

@Component
public class TaskPermissionHelper {

    public void validateUpdatePermission(Task task, User user) {
        if (!canUpdate(task, user)) {
            throw new TaskPermissionException(user.getUsername(), task.getId(), "update");
        }
    }

    public void validateDeletePermission(Task task, User user) {
        if (!canDelete(task, user)) {
            throw new TaskPermissionException(user.getUsername(), task.getId(), "delete");
        }
    }

    public void validateReassignPermission(Task task, User user) {
        if (!canReassign(task, user)) {
            throw new TaskPermissionException(user.getUsername(), task.getId(), "reassign");
        }
    }

    public boolean canUpdate(Task task, User user) {
        return isCreator(task, user) || isAssignee(task, user);
    }

    public boolean canDelete(Task task, User user) {
        return isCreator(task, user);
    }

    public boolean canReassign(Task task, User user) {
        return isCreator(task, user);
    }

    public boolean canView(Task task, User user) {
        return isCreator(task, user) || isAssignee(task, user);
    }

    private boolean isCreator(Task task, User user) {
        return task.getCreatedBy().equals(user);
    }

    private boolean isAssignee(Task task, User user) {
        return task.getAssignedTo().equals(user);
    }
}
