package br.com.rafaelvieira.taskmanagement.controller;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.PomodoroSession;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskCreateRecord;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskRecord;
import br.com.rafaelvieira.taskmanagement.repository.PomodoroSessionRepository;
import br.com.rafaelvieira.taskmanagement.service.TaskService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController implements SwaggerTaskController {

    private final TaskService taskService;
    private final PomodoroSessionRepository pomodoroSessionRepository;

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

    @PatchMapping("/{id}/extend")
    public ResponseEntity<@NotNull TaskRecord> extendTask(
            @PathVariable("id") Long id,
            @org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid
                    br.com.rafaelvieira.taskmanagement.domain.records.TaskExtensionRecord
                            extension) {
        TaskRecord extendedTask = taskService.extendTask(id, extension);
        return ResponseEntity.ok(extendedTask);
    }

    @GetMapping("/{id}/elapsed")
    public ResponseEntity<@NotNull Map<String, Object>> getReadableElapsed(@PathVariable Long id) {
        var task = taskService.getTaskById(id);
        long base = task.mainElapsedSeconds() == null ? 0L : task.mainElapsedSeconds();
        if (task.mainStartedAt() != null && task.status() == TaskStatus.IN_PROGRESS) {
            base +=
                    Duration.between(task.mainStartedAt(), java.time.LocalDateTime.now())
                            .getSeconds();
        }
        Duration d = Duration.ofSeconds(base);
        String readable =
                String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
        return ResponseEntity.ok(Map.of("seconds", base, "readable", readable));
    }

    @GetMapping("/{id}/pomodoros")
    public ResponseEntity<java.util.List<Map<String, Object>>> getPomodoroHistory(
            @PathVariable Long id) {
        var sessions = pomodoroSessionRepository.findByTask(id);
        long total =
                sessions.stream()
                        .mapToLong(
                                s -> s.getDurationSeconds() == null ? 0L : s.getDurationSeconds())
                        .sum();
        var list =
                sessions.stream()
                        .map(
                                s -> {
                                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                                    m.put("id", s.getId());
                                    m.put("startedAt", s.getStartedAt());
                                    m.put("endedAt", s.getEndedAt());
                                    m.put("durationSeconds", s.getDurationSeconds());
                                    m.put("active", s.isActive());
                                    return m;
                                })
                        .toList();
        // Add synthetic summary entry at index 0
        Map<String, Object> summary = new java.util.LinkedHashMap<>();
        summary.put("totalPomodoroSeconds", total);
        summary.put("sessions", list.size());
        var withSummary = new java.util.ArrayList<Map<String, Object>>();
        withSummary.add(summary);
        withSummary.addAll(list);
        return ResponseEntity.ok(withSummary);
    }

    @PatchMapping("/{id}/pomodoro/abort")
    public ResponseEntity<TaskRecord> abortPomodoro(@PathVariable Long id) {
        var task = taskService.getTaskById(id);
        var active = pomodoroSessionRepository.findActiveByTask(id);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (PomodoroSession s : active) {
            s.setEndedAt(now);
            if (s.getStartedAt() != null) {
                s.setDurationSeconds(
                        java.time.Duration.between(s.getStartedAt(), now).getSeconds());
            }
        }
        pomodoroSessionRepository.saveAll(active);
        // Force resume if was paused
        if (task.status() == TaskStatus.IN_PAUSE) {
            taskService.changeTaskStatus(id, TaskStatus.IN_PROGRESS);
        }
        return ResponseEntity.ok(taskService.getTaskById(id));
    }
}
