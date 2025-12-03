package br.com.rafaelvieira.taskmanagement.web.dto;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @NotBlank(message = "A descricao e obrigatoria")
    private String description;

    @NotNull(message = "O status e obrigatorio")
    private TaskStatus status;

    @NotNull(message = "A prioridade e obrigatoria")
    private Priority priority;

    @NotNull(message = "A categoria e obrigatoria")
    private Long categoryId;

    private Long assignedUserId;

    @NotNull(message = "A data de vencimento e obrigatoria")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dueDate;

    // New scheduling fields
    @NotNull(message = "A data de agendamento e obrigatoria")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime scheduledStartAt;

    @NotNull(message = "O tempo de pomodoro e obrigatorio")
    @Positive(message = "O tempo de pomodoro deve ser maior que zero")
    private Integer pomodoroMinutes;

    @NotNull(message = "O tempo de pausa e obrigatorio")
    @Positive(message = "O tempo de pausa deve ser maior que zero")
    private Integer pomodoroBreakMinutes;

    // Tempo de execução alvo (minutos)
    @NotNull(message = "O tempo de execucao e obrigatorio")
    @Positive(message = "O tempo de execucao deve ser maior que zero")
    private Integer executionTimeMinutes;

    // Squad ID (opcional - para LEAD/ADMIN atribuir task a squad)
    private Long squadId;
}
