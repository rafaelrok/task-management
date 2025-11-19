package br.com.rafaelvieira.taskmanagement.service;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskCreateRecord;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskRecord;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    TaskRecord createTask(TaskCreateRecord taskCreate);

    TaskRecord updateTask(Long id, TaskCreateRecord taskCreate);

    TaskRecord getTaskById(Long id);

    List<TaskRecord> getAllTasks();

    List<TaskRecord> getTasksByStatus(TaskStatus status);

    List<TaskRecord> getTasksByPriority(Priority priority);

    List<TaskRecord> getTasksByCategoryId(Long categoryId);

    List<TaskRecord> getTasksByUserId(Long userId);

    List<TaskRecord> getOverdueTasks();

    void deleteTask(Long id);

    TaskRecord changeTaskStatus(Long id, TaskStatus newStatus);

    long countTasksByStatus(TaskStatus status);

    long countTasksByPriority(Priority priority);

    List<TaskRecord> getTasksDueToday();

    Page<TaskRecord> searchTasks(
            br.com.rafaelvieira.taskmanagement.web.dto.TaskFilterForm filter, Pageable pageable);
}
