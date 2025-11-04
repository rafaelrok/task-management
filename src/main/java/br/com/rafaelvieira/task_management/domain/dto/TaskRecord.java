package br.com.rafaelvieira.task_management.domain.dto;

import br.com.rafaelvieira.task_management.domain.enums.Priority;
import br.com.rafaelvieira.task_management.domain.enums.TaskStatus;

import java.time.LocalDateTime;

public record TaskRecord(Long id,
                         String title,
                         String description,
                         TaskStatus status,
                         Priority priority,
                         String categoryName,
                         String assignedUserName,
                         LocalDateTime createdAt,
                         LocalDateTime updatedAt,
                         LocalDateTime dueDate,
                         boolean overdue
) {}
