package br.com.rafaelvieira.taskmanagement.service.impl;

import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.repository.TaskRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TaskTimerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskTimerJob.class);

    private final TaskRepository taskRepository;

    // Run every 10 seconds to update elapsed time and check pomodoro transitions
    @Scheduled(fixedDelayString = "${task.timer.fixedDelayMs:10000}")
    @Transactional
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        var tasks = taskRepository.findAll();
        boolean changed = false;
        for (Task t : tasks) {
            try {
                if (updateRunningTask(t, now)) {
                    changed = true;
                }
                if (processPomodoroCompletion(t, now)) {
                    changed = true;
                }
                if (processBreakCompletion(t, now)) {
                    changed = true;
                }
                if (checkOverdue(t, now)) {
                    changed = true;
                }
            } catch (ResourceNotFoundException e) {
                LOGGER.error("Error processing task ID {}: {}", t.getId(), e.getMessage());
            }
        }
        if (changed) {
            taskRepository.saveAll(tasks);
        }
    }

    private boolean updateRunningTask(Task t, LocalDateTime now) {
        if (t.getStatus() != TaskStatus.IN_PROGRESS || t.getMainStartedAt() == null) {
            return false;
        }
        long delta = java.time.Duration.between(t.getMainStartedAt(), now).getSeconds();
        if (delta < 1) {
            return false;
        }
        t.setMainElapsedSeconds(
                (t.getMainElapsedSeconds() == null ? 0L : t.getMainElapsedSeconds()) + delta);
        t.setMainStartedAt(now);
        return true;
    }

    private boolean processPomodoroCompletion(Task t, LocalDateTime now) {
        if (t.getStatus() != TaskStatus.IN_PROGRESS
                || t.getPomodoroUntil() == null
                || now.isBefore(t.getPomodoroUntil())) {
            return false;
        }
        // Pomodoro ended: auto-pause and start break
        t.setPomodoroUntil(null);
        t.setStatus(TaskStatus.IN_PAUSE);
        // Pause main timer
        if (t.getMainStartedAt() != null) {
            long delta = java.time.Duration.between(t.getMainStartedAt(), now).getSeconds();
            t.setMainElapsedSeconds(
                    (t.getMainElapsedSeconds() == null ? 0L : t.getMainElapsedSeconds()) + delta);
            t.setMainStartedAt(null);
        }
        // Start break timer: repurpose pomodoroUntil for break countdown
        int breakMin = t.getPomodoroBreakMinutes() != null ? t.getPomodoroBreakMinutes() : 5;
        t.setPomodoroUntil(now.plusMinutes(breakMin));
        return true;
    }

    private boolean processBreakCompletion(Task t, LocalDateTime now) {
        if (t.getStatus() != TaskStatus.IN_PAUSE
                || t.getPomodoroUntil() == null
                || now.isBefore(t.getPomodoroUntil())) {
            return false;
        }
        // Break ended: auto-resume IN_PROGRESS and start new pomodoro
        t.setStatus(TaskStatus.IN_PROGRESS);
        t.setMainStartedAt(now);
        int pomoMin = t.getPomodoroMinutes() != null ? t.getPomodoroMinutes() : 25;
        t.setPomodoroUntil(now.plusMinutes(pomoMin));
        return true;
    }

    private boolean checkOverdue(Task t, LocalDateTime now) {
        if (t.getExecutionTimeMinutes() == null || t.getExecutionTimeMinutes() <= 0) {
            return false;
        }
        if (t.getStatus() == TaskStatus.DONE
                || t.getStatus() == TaskStatus.CANCELLED
                || t.getStatus() == TaskStatus.OVERDUE) {
            return false;
        }
        long targetSeconds = t.getExecutionTimeMinutes() * 60L;
        long elapsed = t.getMainElapsedSeconds() == null ? 0L : t.getMainElapsedSeconds();
        if (t.getMainStartedAt() != null) {
            elapsed += java.time.Duration.between(t.getMainStartedAt(), now).getSeconds();
        }
        if (elapsed >= targetSeconds) {
            if (t.getMainStartedAt() != null) {
                long delta = java.time.Duration.between(t.getMainStartedAt(), now).getSeconds();
                t.setMainElapsedSeconds(
                        (t.getMainElapsedSeconds() == null ? 0L : t.getMainElapsedSeconds())
                                + delta);
                t.setMainStartedAt(null);
            }
            t.setStatus(TaskStatus.OVERDUE);
            t.setPomodoroUntil(null);
            return true;
        }
        return false;
    }
}
