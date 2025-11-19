package br.com.rafaelvieira.taskmanagement.domain.records;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record TaskCreateRecord(
        @NotBlank(message = "Title is required")
                @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
                String title,
        @Size(max = 8000, message = "Description cannot exceed 8000 characters") String description,
        TaskStatus status,
        Priority priority,
        Long categoryId,
        Long assignedUserId,
        LocalDateTime dueDate,
        LocalDateTime scheduledStartAt,
        Integer pomodoroMinutes,
        Integer pomodoroBreakMinutes,
        Integer executionTimeMinutes) {

    public TaskCreateRecord(
            String title,
            String description,
            TaskStatus status,
            Priority priority,
            Long categoryId,
            Long assignedUserId,
            LocalDateTime dueDate) {
        this(
                title,
                description,
                status,
                priority,
                categoryId,
                assignedUserId,
                dueDate,
                null,
                null,
                null,
                null);
    }
}
