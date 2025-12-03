package br.com.rafaelvieira.taskmanagement.domain.records;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import java.time.LocalDateTime;

/*
* Record que representa uma tarefa no sistema de gestão de tarefas.
*
* @author
 Rafael Vieira (rafaelrok)
* @since
 2024-06-01
*/
public record TaskRecord(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Priority priority,
        String categoryName,
        String assignedUserName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime dueDate,
        boolean overdue,
        Long categoryId,
        Long assignedUserId,
        LocalDateTime scheduledStartAt,
        Integer pomodoroMinutes,
        Integer pomodoroBreakMinutes,
        Integer executionTimeMinutes,
        LocalDateTime mainStartedAt,
        Long mainElapsedSeconds,
        LocalDateTime pomodoroUntil,
        Integer extraTimeMinutes,
        String extensionJustification,
        Long squadId,
        Long squadLeadId) {}
