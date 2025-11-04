package br.com.rafaelvieira.task_management.service;

import br.com.rafaelvieira.task_management.domain.dto.TaskCreateRecord;
import br.com.rafaelvieira.task_management.domain.dto.TaskRecord;
import br.com.rafaelvieira.task_management.domain.enums.Priority;
import br.com.rafaelvieira.task_management.domain.enums.TaskStatus;

import java.util.List;

public interface TaskService {

    /**
     * Create a new task from the given .
     *
     * @param taskCreate the  containing the task information
     * @return the created task record
     */
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
}
