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

    TaskRecord createTask(TaskCreateRecord taskCreate, Long squadId);

    TaskRecord updateTask(Long id, TaskCreateRecord taskCreate);

    TaskRecord updateTask(Long id, TaskCreateRecord taskCreate, User currentUser);

    TaskRecord getTaskById(Long id);

    List<TaskRecord> getAllTasks();

    List<TaskRecord> getVisibleTasks(User currentUser);

    List<TaskRecord> getTasksBySquadId(Long squadId);

    List<TaskRecord> getTasksByStatus(TaskStatus status);

    List<TaskRecord> getTasksByPriority(Priority priority);

    List<TaskRecord> getTasksByCategoryId(Long categoryId);

    List<TaskRecord> getTasksByUserId(Long userId);

    List<TaskRecord> getOverdueTasks();

    void deleteTask(Long id);

    void deleteTask(Long id, User currentUser);

    TaskRecord changeTaskStatus(Long id, TaskStatus newStatus);

    TaskRecord changeTaskStatus(Long id, TaskStatus newStatus, User currentUser);

    boolean canUserModifyTask(User user, Task task);

    boolean canUserDeleteTask(User user, Task task);

    boolean canUserControlTimer(User user, Task task);

    long countTasksByStatus(TaskStatus status);

    long countTasksByPriority(Priority priority);

    List<TaskRecord> getTasksDueToday();

    Page<TaskRecord> searchTasks(
            br.com.rafaelvieira.taskmanagement.web.dto.TaskFilterForm filter, Pageable pageable);

    Page<TaskRecord> searchTasks(
            br.com.rafaelvieira.taskmanagement.web.dto.TaskFilterForm filter,
            Pageable pageable,
            User currentUser);

    TaskRecord extendTask(
            Long id,
            br.com.rafaelvieira.taskmanagement.domain.records.TaskExtensionRecord extension);

    TaskRecord assignTaskToCurrentUser(Long taskId, User currentUser);

    long countTasksByStatus(TaskStatus status, User currentUser);

    List<TaskRecord> getTasksByStatus(TaskStatus status, User currentUser);

    List<TaskRecord> getOverdueTasks(User currentUser);

    long countTasksByPriority(Priority priority, User currentUser);
}
