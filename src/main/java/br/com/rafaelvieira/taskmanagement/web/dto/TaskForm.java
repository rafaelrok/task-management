package br.com.rafaelvieira.taskmanagement.web.dto;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Setter
@Getter
public class TaskForm {
    private Long id;

    @NotBlank(message = "O titulo e obrigatorio")
    private String title;

    private String description;

    @NotNull(message = "O status e obrigatorio")
    private TaskStatus status;

    @NotNull(message = "A prioridade e obrigatoria")
    private Priority priority;

    private Long categoryId;

    private Long assignedUserId;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dueDate;

    // New scheduling fields
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime scheduledStartAt;

    private Integer pomodoroMinutes;

    private Integer pomodoroBreakMinutes;

    // Tempo de execução alvo (minutos)
    private Integer executionTimeMinutes;
}
