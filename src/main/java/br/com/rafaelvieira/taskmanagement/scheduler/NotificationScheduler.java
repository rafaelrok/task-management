package br.com.rafaelvieira.taskmanagement.scheduler;

import br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import br.com.rafaelvieira.taskmanagement.repository.TaskRepository;
import br.com.rafaelvieira.taskmanagement.service.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 60000) // Check every minute
    @Transactional
    public void checkOverdueTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<Task> overdueTasks = taskRepository.findOverdueTasks(now);

        for (Task task : overdueTasks) {
            // If task is not DONE/CANCELLED, we should notify
            if (task.getStatus() != TaskStatus.DONE && task.getStatus() != TaskStatus.CANCELLED) {

                boolean statusChanged = false;
                if (task.getStatus() != TaskStatus.OVERDUE) {
                    task.setStatus(TaskStatus.OVERDUE);
                    statusChanged = true;
                }

                // Always save if status changed, or if we just want to ensure persistence
                if (statusChanged) {
                    taskRepository.save(task);
                }

                if (task.getAssignedUser() != null) {
                    // Create notification (will trigger toast via SSE)
                    // We add a timestamp to the title to make it distinct if needed,
                    // or just keep it simple. The user wants "re-launch".
                    notificationService.createNotification(
                            "Tarefa Atrasada",
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
}
