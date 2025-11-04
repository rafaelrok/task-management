package br.com.rafaelvieira.task_management.controller;

import br.com.rafaelvieira.task_management.domain.dto.TaskCreateRecord;
import br.com.rafaelvieira.task_management.domain.dto.TaskRecord;
import br.com.rafaelvieira.task_management.domain.enums.Priority;
import br.com.rafaelvieira.task_management.domain.enums.TaskStatus;
import br.com.rafaelvieira.task_management.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskRecord> createTask(@Valid @RequestBody TaskCreateRecord taskCreate) {
        TaskRecord taskRecord = taskService.createTask(taskCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(taskRecord);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskRecord> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskCreateRecord taskCreate) {
        TaskRecord updatedTask = taskService.updateTask(id, taskCreate);
        return ResponseEntity.ok(updatedTask);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskRecord> getTaskById(@PathVariable Long id) {
        TaskRecord task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    @GetMapping
    public ResponseEntity<@NotNull List<TaskRecord>> getAllTasks() {
        List<TaskRecord> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TaskRecord>> getTasksByStatus(@PathVariable TaskStatus status) {
        List<TaskRecord> tasks = taskService.getTasksByStatus(status);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<TaskRecord>> getTasksByPriority(@PathVariable Priority priority) {
        List<TaskRecord> tasks = taskService.getTasksByPriority(priority);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<TaskRecord>> getTasksByCategoryId(@PathVariable Long categoryId) {
        List<TaskRecord> tasks = taskService.getTasksByCategoryId(categoryId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TaskRecord>> getTasksByUserId(@PathVariable Long userId) {
        List<TaskRecord> tasks = taskService.getTasksByUserId(userId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<TaskRecord>> getOverdueTasks() {
        List<TaskRecord> tasks = taskService.getOverdueTasks();
        return ResponseEntity.ok(tasks);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskRecord> changeTaskStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status) {
        TaskRecord updatedTask = taskService.changeTaskStatus(id, status);
        return ResponseEntity.ok(updatedTask);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/{status}")
    public ResponseEntity<Long> countTasksByStatus(@PathVariable TaskStatus status) {
        long count = taskService.countTasksByStatus(status);
        return ResponseEntity.ok(count);
    }
}
