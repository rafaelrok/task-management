package br.com.rafaelvieira.taskmanagement.scheduler;

import br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import br.com.rafaelvieira.taskmanagement.repository.TaskRepository;
import br.com.rafaelvieira.taskmanagement.service.NotificationService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agenda para verificar tarefas e enviar notificações proativas. Executa a cada minuto para: -
 * Identificar tarefas que passaram do prazo e atualizar status para OVERDUE - Alertar sobre tarefas
 * que vão começar em 5 minutos (TASK_STARTING_SOON) - Alertar sobre tarefas com prazo próximo de
 * vencer (TASK_DUE_SOON)
 *
 * <p>Author: Rafael Vieira Since: 25/11/2025
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    // Track tasks that already received starting soon notifications (to avoid
    // duplicates)
    private final Set<Long> notifiedStartingSoon = new HashSet<>();
    // Track tasks that already received due soon notifications
    private final Set<Long> notifiedDueSoon = new HashSet<>();

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkOverdueTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<Task> overdueTasks = taskRepository.findOverdueTasks(now);

        for (Task task : overdueTasks) {
            if (task.getStatus() != TaskStatus.DONE && task.getStatus() != TaskStatus.CANCELLED) {

                boolean statusChanged = false;
                if (task.getStatus() != TaskStatus.OVERDUE) {
                    task.setStatus(TaskStatus.OVERDUE);
                    statusChanged = true;
                }

                if (statusChanged) {
                    taskRepository.save(task);
                }

                if (task.getAssignedUser() != null) {
                    notificationService.createStickyNotification(
                            "🚨 Tarefa Atrasada",
                            "A tarefa '"
                                    + task.getTitle()
                                    + "' está atrasada! Verifique imediatamente.",
                            NotificationType.TASK_OVERDUE,
                            task.getId(),
                            task.getAssignedUser());
                }
            }
        }
    }

    /**
     * Verifica tarefas agendadas que vão começar em 5 minutos. Notifica o usuário para se preparar
     * para o início da tarefa.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional(readOnly = true)
    public void checkTasksStartingSoon() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinutesFromNow = now.plusMinutes(5);

        List<Task> allTasks = taskRepository.findAll();

        for (Task task : allTasks) {
            // Só verifica tarefas TODO com scheduledStartAt definido
            if (task.getStatus() != TaskStatus.TODO) {
                continue;
            }
            if (task.getScheduledStartAt() == null) {
                continue;
            }
            // Já notificou sobre esta tarefa?
            if (notifiedStartingSoon.contains(task.getId())) {
                continue;
            }

            // Verifica se vai começar nos próximos 5 minutos
            LocalDateTime scheduledStart = task.getScheduledStartAt();
            if (scheduledStart.isAfter(now) && scheduledStart.isBefore(fiveMinutesFromNow)) {
                log.info(
                        "Task ID {} '{}' is starting soon at {}",
                        task.getId(),
                        task.getTitle(),
                        scheduledStart);

                if (task.getAssignedUser() != null) {
                    long minutesUntilStart =
                            java.time.Duration.between(now, scheduledStart).toMinutes();
                    notificationService.createNotification(
                            "⏱️ Tarefa Começando em Breve",
                            "A tarefa '"
                                    + task.getTitle()
                                    + "' está agendada para começar em "
                                    + (minutesUntilStart + 1)
                                    + " minuto(s). Prepare-se!",
                            NotificationType.TASK_STARTING_SOON,
                            task.getId(),
                            task.getAssignedUser());

                    notifiedStartingSoon.add(task.getId());
                }
            }
        }

        // Limpa notificações antigas (tarefas que já começaram ou foram removidas)
        cleanupStartingSoonNotifications();
    }

    /**
     * Verifica tarefas com prazo de vencimento próximo (próximas 2 horas). Apenas para tarefas
     * ativas (TODO, IN_PROGRESS, IN_PAUSE, PENDING).
     */
    @Scheduled(fixedRate = 300000) // A cada 5 minutos
    @Transactional(readOnly = true)
    public void checkTasksDueSoon() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoHoursFromNow = now.plusHours(2);

        List<Task> allTasks = taskRepository.findAll();

        for (Task task : allTasks) {
            // Só verifica tarefas ativas
            if (task.getStatus() == TaskStatus.DONE
                    || task.getStatus() == TaskStatus.CANCELLED
                    || task.getStatus() == TaskStatus.OVERDUE) {
                continue;
            }
            if (task.getDueDate() == null) {
                continue;
            }
            // Já notificou sobre esta tarefa?
            if (notifiedDueSoon.contains(task.getId())) {
                continue;
            }

            // Verifica se vai vencer nas próximas 2 horas
            LocalDateTime dueDate = task.getDueDate();
            if (dueDate.isAfter(now) && dueDate.isBefore(twoHoursFromNow)) {
                log.info(
                        "Task ID {} '{}' is due soon at {}",
                        task.getId(),
                        task.getTitle(),
                        dueDate);

                if (task.getAssignedUser() != null) {
                    long minutesUntilDue = java.time.Duration.between(now, dueDate).toMinutes();
                    String timeStr;
                    if (minutesUntilDue >= 60) {
                        long hours = minutesUntilDue / 60;
                        long mins = minutesUntilDue % 60;
                        timeStr =
                                hours + " hora(s)" + (mins > 0 ? " e " + mins + " minuto(s)" : "");
                    } else {
                        timeStr = minutesUntilDue + " minuto(s)";
                    }

                    notificationService.createNotification(
                            "⚠️ Prazo Próximo",
                            "A tarefa '"
                                    + task.getTitle()
                                    + "' vence em "
                                    + timeStr
                                    + ". Finalize-a a tempo!",
                            NotificationType.TASK_DUE_SOON,
                            task.getId(),
                            task.getAssignedUser());

                    notifiedDueSoon.add(task.getId());
                }
            }
        }

        // Limpa notificações antigas
        cleanupDueSoonNotifications();
    }

    /** Limpa o cache de tarefas notificadas sobre início próximo. */
    private void cleanupStartingSoonNotifications() {
        LocalDateTime now = LocalDateTime.now();
        notifiedStartingSoon.removeIf(
                taskId -> {
                    Task task = taskRepository.findById(taskId).orElse(null);
                    if (task == null) {
                        return true; // Task foi removida
                    }
                    // Remove se já começou ou não é mais TODO
                    return task.getStatus() != TaskStatus.TODO
                            || task.getScheduledStartAt() == null
                            || task.getScheduledStartAt().isBefore(now);
                });
    }

    /** Limpa o cache de tarefas notificadas sobre prazo próximo. */
    private void cleanupDueSoonNotifications() {
        LocalDateTime now = LocalDateTime.now();
        notifiedDueSoon.removeIf(
                taskId -> {
                    Task task = taskRepository.findById(taskId).orElse(null);
                    if (task == null) {
                        return true; // Task foi removida
                    }
                    // Remove se já venceu ou status final
                    return task.getStatus() == TaskStatus.DONE
                            || task.getStatus() == TaskStatus.CANCELLED
                            || task.getStatus() == TaskStatus.OVERDUE
                            || (task.getDueDate() != null && task.getDueDate().isBefore(now));
                });
    }
}
