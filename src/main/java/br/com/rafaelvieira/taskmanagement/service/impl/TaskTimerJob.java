package br.com.rafaelvieira.taskmanagement.service.impl;

import br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.repository.TaskRepository;
import br.com.rafaelvieira.taskmanagement.service.NotificationService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job responsável pelo gerenciamento automático de timers das tarefas. Executa a cada 5 segundos
 * para: - Auto-iniciar tarefas agendadas - Atualizar tempo decorrido - Gerenciar ciclos
 * pomodoro/break - Verificar tempo excedido (PENDING/OVERDUE) - Monitorar datas de vencimento
 *
 * @author Rafael Vieira
 * @see <a href='https://rafaelvieira.com.br'>Rafael Vieira</a>
 * @since 14/06/2024
 */
@Component
@RequiredArgsConstructor
public class TaskTimerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskTimerJob.class);

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelayString = "${task.timer.fixedDelayMs:5000}")
    @Transactional
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        var tasks = taskRepository.findAll();
        boolean changed = false;

        for (Task t : tasks) {
            try {
                // Auto-start tasks when scheduled time arrives
                if (autoStartScheduledTask(t, now)) {
                    changed = true;
                }
                if (updateRunningTask(t, now)) {
                    changed = true;
                }
                if (processPomodoroCompletion(t, now)) {
                    changed = true;
                }
                if (processBreakCompletion(t, now)) {
                    changed = true;
                }
                // Verifica se tempo de execução foi excedido
                if (checkTimeCompleted(t, now)) {
                    changed = true;
                }
                // Verifica se PENDING deve virar OVERDUE (data de vencimento passou)
                if (checkPendingToOverdue(t, now)) {
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

    private boolean autoStartScheduledTask(Task t, LocalDateTime now) {
        if (t.getStatus() != TaskStatus.TODO) {
            return false;
        }
        if (t.getScheduledStartAt() == null || t.getScheduledStartAt().isAfter(now)) {
            return false;
        }
        if (t.getExecutionTimeMinutes() == null || t.getExecutionTimeMinutes() <= 0) {
            return false;
        }
        if (t.getPomodoroMinutes() == null || t.getPomodoroMinutes() <= 0) {
            return false;
        }

        LOGGER.info("Auto-starting scheduled task ID {}: '{}'", t.getId(), t.getTitle());
        t.setStatus(TaskStatus.IN_PROGRESS);
        t.setMainStartedAt(now);
        t.setMainElapsedSeconds(0L);
        t.setPomodoroUntil(now.plusMinutes(t.getPomodoroMinutes()));

        if (t.getAssignedUser() != null) {
            notificationService.createNotification(
                    "Tarefa Iniciada Automaticamente",
                    "A tarefa '"
                            + t.getTitle()
                            + "' foi iniciada automaticamente no horário agendado.",
                    NotificationType.TASK_STARTED,
                    t.getId(),
                    t.getAssignedUser());
        }

        return true;
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

        t.setPomodoroUntil(null);
        t.setStatus(TaskStatus.IN_PAUSE);

        if (t.getMainStartedAt() != null) {
            long delta = java.time.Duration.between(t.getMainStartedAt(), now).getSeconds();
            t.setMainElapsedSeconds(
                    (t.getMainElapsedSeconds() == null ? 0L : t.getMainElapsedSeconds()) + delta);
            t.setMainStartedAt(null);
        }

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

        t.setStatus(TaskStatus.IN_PROGRESS);
        t.setMainStartedAt(now);
        int pomoMin = t.getPomodoroMinutes() != null ? t.getPomodoroMinutes() : 25;
        t.setPomodoroUntil(now.plusMinutes(pomoMin));
        return true;
    }

    /**
     * Verifica se o tempo de execução foi completado. Se dueDate ainda não passou -> PENDING (azul,
     * aguardando finalização) Se dueDate já passou ou não existe -> OVERDUE (vermelho)
     */
    private boolean checkTimeCompleted(Task t, LocalDateTime now) {
        if (t.getStatus() != TaskStatus.IN_PROGRESS && t.getStatus() != TaskStatus.IN_PAUSE) {
            return false;
        }
        if (t.getExecutionTimeMinutes() == null || t.getExecutionTimeMinutes() <= 0) {
            return false;
        }

        int totalMinutes = t.getExecutionTimeMinutes();
        if (t.getExtraTimeMinutes() != null) {
            totalMinutes += t.getExtraTimeMinutes();
        }
        long targetSeconds = totalMinutes * 60L;

        long elapsed = t.getMainElapsedSeconds() == null ? 0L : t.getMainElapsedSeconds();
        if (t.getMainStartedAt() != null) {
            elapsed += java.time.Duration.between(t.getMainStartedAt(), now).getSeconds();
        }

        if (elapsed < targetSeconds) {
            return false;
        }

        if (t.getMainStartedAt() != null) {
            long delta = java.time.Duration.between(t.getMainStartedAt(), now).getSeconds();
            t.setMainElapsedSeconds(
                    (t.getMainElapsedSeconds() == null ? 0L : t.getMainElapsedSeconds()) + delta);
            t.setMainStartedAt(null);
        }
        t.setPomodoroUntil(null);

        boolean withinDueDate = t.getDueDate() != null && now.isBefore(t.getDueDate());

        if (withinDueDate) {
            t.setStatus(TaskStatus.PENDING);
            LOGGER.info(
                    "Task ID {} '{}' completed execution time, set to PENDING (within due date)",
                    t.getId(),
                    t.getTitle());

            if (t.getAssignedUser() != null) {
                notificationService.createStickyNotification(
                        "Tempo de Execução Finalizado",
                        "A tarefa '"
                                + t.getTitle()
                                + "' completou o tempo de execução. "
                                + "Você ainda está dentro do prazo. Finalize ou estenda o tempo.",
                        NotificationType.TASK_PENDING,
                        t.getId(),
                        t.getAssignedUser());
            }
        } else {
            t.setStatus(TaskStatus.OVERDUE);
            LOGGER.info(
                    "Task ID {} '{}' completed execution time, set to OVERDUE (past due date or no"
                            + " due date)",
                    t.getId(),
                    t.getTitle());

            if (t.getAssignedUser() != null) {
                notificationService.createStickyNotification(
                        "Tarefa Atrasada",
                        "A tarefa '"
                                + t.getTitle()
                                + "' está atrasada! O tempo de execução foi excedido e o prazo de"
                                + " vencimento já passou.",
                        NotificationType.TASK_OVERDUE,
                        t.getId(),
                        t.getAssignedUser());
            }
        }

        return true;
    }

    private boolean checkPendingToOverdue(Task t, LocalDateTime now) {
        if (t.getStatus() != TaskStatus.PENDING) {
            return false;
        }

        if (t.getDueDate() == null) {
            t.setStatus(TaskStatus.OVERDUE);
            LOGGER.info(
                    "Task ID {} '{}' changed from PENDING to OVERDUE (no due date)",
                    t.getId(),
                    t.getTitle());
            return true;
        }

        if (now.isAfter(t.getDueDate())) {
            t.setStatus(TaskStatus.OVERDUE);
            LOGGER.info(
                    "Task ID {} '{}' changed from PENDING to OVERDUE (due date passed)",
                    t.getId(),
                    t.getTitle());

            if (t.getAssignedUser() != null) {
                notificationService.createStickyNotification(
                        "Tarefa Vencida",
                        "A tarefa '"
                                + t.getTitle()
                                + "' estava aguardando finalização, mas o prazo de vencimento"
                                + " passou. Finalize ou estenda urgentemente!",
                        NotificationType.TASK_OVERDUE,
                        t.getId(),
                        t.getAssignedUser());
            }

            return true;
        }

        return false;
    }
}
