package br.com.rafaelvieira.taskmanagement.event.listener;

import br.com.rafaelvieira.taskmanagement.event.TaskEvent;
import br.com.rafaelvieira.taskmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener para eventos de tarefas que cria notificações para usuários atribuídos
 *
 * @author Rafael Vieira (rafaelrok)
 * @since 2024-06-15
 */
@Component
@RequiredArgsConstructor
public class TaskEventListener {

    private final NotificationService notificationService;

    @Async
    @EventListener
    public void onTaskEvent(TaskEvent event) {
        if (event.getTask().getAssignedUser() == null) {
            return;
        }
        String richMessage =
                String.format(
                        "%s | Status: %s | Prioridade: %s",
                        event.getMessage(),
                        event.getTask().getStatus(),
                        event.getTask().getPriority());

        notificationService.createNotification(
                event.getTask().getTitle(),
                richMessage,
                event.getType(),
                event.getTask().getId(),
                event.getTask().getAssignedUser());
    }
}
