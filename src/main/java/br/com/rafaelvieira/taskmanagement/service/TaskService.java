package br.com.rafaelvieira.taskmanagement.service;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskCreateRecord;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskRecord;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {

        TaskRecord createTask(TaskCreateRecord taskCreate);

        /** Creates a task with optional squad assignment (for LEAD/ADMIN only) */
        TaskRecord createTask(TaskCreateRecord taskCreate, Long squadId);

        TaskRecord updateTask(Long id, TaskCreateRecord taskCreate);

        /**
         * Updates a task with role-based restrictions. MEMBERs cannot change dueDate on
         * squad tasks.
         */
        TaskRecord updateTask(Long id, TaskCreateRecord taskCreate, User currentUser);

        TaskRecord getTaskById(Long id);

        List<TaskRecord> getAllTasks();

        /**
         * Returns tasks visible to the current user based on their role: - ADMIN: all
         * tasks - LEAD:
         * personal tasks + tasks from squads they lead - MEMBER: personal tasks + tasks
         * from squads
         * they belong to
         */
        List<TaskRecord> getVisibleTasks(User currentUser);

        /** Returns tasks from a specific squad (for dashboard) */
        List<TaskRecord> getTasksBySquadId(Long squadId);

        List<TaskRecord> getTasksByStatus(TaskStatus status);

        List<TaskRecord> getTasksByPriority(Priority priority);

        List<TaskRecord> getTasksByCategoryId(Long categoryId);

        List<TaskRecord> getTasksByUserId(Long userId);

        List<TaskRecord> getOverdueTasks();

        void deleteTask(Long id);

        /**
         * Deletes a task with role-based restrictions. MEMBERs cannot delete squad
         * tasks.
         */
        void deleteTask(Long id, User currentUser);

        TaskRecord changeTaskStatus(Long id, TaskStatus newStatus);

        /**
         * Changes task status with role-based restrictions. MEMBERs cannot control
         * timers on squad
         * tasks.
         */
        TaskRecord changeTaskStatus(Long id, TaskStatus newStatus, User currentUser);

        /** Checks if the user can modify the task based on their role */
        boolean canUserModifyTask(User user, Task task);

        /** Checks if the user can delete the task based on their role */
        boolean canUserDeleteTask(User user, Task task);

        /** Checks if the user can control the timer on the task based on their role */
        boolean canUserControlTimer(User user, Task task);

        long countTasksByStatus(TaskStatus status);

        long countTasksByPriority(Priority priority);

        List<TaskRecord> getTasksDueToday();

        Page<TaskRecord> searchTasks(
                        br.com.rafaelvieira.taskmanagement.web.dto.TaskFilterForm filter, Pageable pageable);

        /** Search tasks with role-based visibility filter */
        Page<TaskRecord> searchTasks(
                        br.com.rafaelvieira.taskmanagement.web.dto.TaskFilterForm filter,
                        Pageable pageable,
                        User currentUser);

        TaskRecord extendTask(
                        Long id,
                        br.com.rafaelvieira.taskmanagement.domain.records.TaskExtensionRecord extension);

        /** Assigns the task to the current user (for "Assumir" button) */
        TaskRecord assignTaskToCurrentUser(Long taskId, User currentUser);

        long countTasksByStatus(TaskStatus status, User currentUser);

        List<TaskRecord> getTasksByStatus(TaskStatus status, User currentUser);

        List<TaskRecord> getOverdueTasks(User currentUser);

        long countTasksByPriority(Priority priority, User currentUser);
}
