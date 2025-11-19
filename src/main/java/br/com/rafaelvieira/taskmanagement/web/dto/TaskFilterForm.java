package br.com.rafaelvieira.taskmanagement.web.dto;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TaskFilterForm {
    private TaskStatus status;
    private Priority priority;
    private Long userId;
    private Long categoryId;
    private LocalDate createdFrom;
    private LocalDate createdTo;
    private LocalDate dueFrom;
    private LocalDate dueTo;
}
