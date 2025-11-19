package br.com.rafaelvieira.taskmanagement.domain.records;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Record para estender o tempo de execução de uma tarefa.
 *
 * @param extraTimeMinutes Tempo extra em minutos a ser adicionado
 * @param justification Justificativa para a extensão
 * @param scheduledStartAt Nova data de início (opcional)
 * @param dueDate Nova data de vencimento (opcional)
 */
public record TaskExtensionRecord(
        @NotNull(message = "Extra time is required")
                @Min(value = 1, message = "Extra time must be at least 1 minute")
                Integer extraTimeMinutes,
        @Size(max = 2000, message = "Justification cannot exceed 2000 characters")
                String justification,
        LocalDateTime scheduledStartAt,
        LocalDateTime dueDate) {}
