package br.com.rafaelvieira.taskmanagement.controller;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskCreateRecord;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskRecord;
import br.com.rafaelvieira.taskmanagement.service.TaskService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor

public class TaskController implements SwaggerTaskController {

    private final TaskService taskService;

    @Override


    public ResponseEntity<@NotNull TaskRecord> createTask(TaskCreateRecord taskCreate) {
        TaskRecord taskRecord = taskService.createTask(taskCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(taskRecord);
    }

    @Override


    public ResponseEntity<@NotNull TaskRecord> updateTask(Long id, TaskCreateRecord taskCreate) {
        TaskRecord updatedTask = taskService.updateTask(id, taskCreate);
        return ResponseEntity.ok(updatedTask);
    }

    @Override


    public ResponseEntity<@NotNull TaskRecord> getTaskById(Long id) {
        TaskRecord task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    @Override


    public ResponseEntity<@NotNull List<TaskRecord>> getAllTasks() {
        List<TaskRecord> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    @Override


    public ResponseEntity<@NotNull List<TaskRecord>> getTasksByStatus(TaskStatus status) {
        List<TaskRecord> tasks = taskService.getTasksByStatus(status);
        return ResponseEntity.ok(tasks);
    }

    @Override


    public ResponseEntity<@NotNull List<TaskRecord>> getTasksByPriority(Priority priority) {
        List<TaskRecord> tasks = taskService.getTasksByPriority(priority);
        return ResponseEntity.ok(tasks);
    }

    @Override


    public ResponseEntity<@NotNull List<TaskRecord>> getTasksByCategoryId(Long categoryId) {
        List<TaskRecord> tasks = taskService.getTasksByCategoryId(categoryId);
        return ResponseEntity.ok(tasks);
    }

    @Override


    public ResponseEntity<@NotNull List<TaskRecord>> getTasksByUserId(Long userId) {
        List<TaskRecord> tasks = taskService.getTasksByUserId(userId);
        return ResponseEntity.ok(tasks);
    }

    @Override


    public ResponseEntity<@NotNull List<TaskRecord>> getOverdueTasks() {
        List<TaskRecord> tasks = taskService.getOverdueTasks();
        return ResponseEntity.ok(tasks);
    }

    @Override


    public ResponseEntity<@NotNull TaskRecord> changeTaskStatus(Long id, TaskStatus status) {
        TaskRecord updatedTask = taskService.changeTaskStatus(id, status);
        return ResponseEntity.ok(updatedTask);
    }

    @Override


    public ResponseEntity<@NotNull Void> deleteTask(Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @Override


    public ResponseEntity<@NotNull Long> countTasksByStatus(TaskStatus status) {
        long count = taskService.countTasksByStatus(status);
        return ResponseEntity.ok(count);
    }
}
