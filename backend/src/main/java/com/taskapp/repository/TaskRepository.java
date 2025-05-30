package com.taskapp.repository;

import com.taskapp.entity.Task;
import com.taskapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedTo(User assignedTo);
    List<Task> findByCreatedBy(User createdBy);
    List<Task> findByAssignedToOrCreatedBy(User assignedTo, User createdBy);
}
