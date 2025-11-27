package br.com.rafaelvieira.taskmanagement.event;

import br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Evento relacionado a tarefas
 *
 * @author Rafael Vieira (rafaelrok)
 * @since 2024-06-15
 */
@Getter
public class TaskEvent extends ApplicationEvent {

    private final Task task;
    private final NotificationType type;
    private final String message;

    public TaskEvent(Object source, Task task, NotificationType type, String message) {
        super(source);
        this.task = task;
        this.type = type;
        this.message = message;
    }
}
