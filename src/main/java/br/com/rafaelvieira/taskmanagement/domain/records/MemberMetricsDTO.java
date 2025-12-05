package br.com.rafaelvieira.taskmanagement.domain.records;

import java.time.LocalDateTime;

public record MemberMetricsDTO(
        Long userId,
        String fullName,
        String username,
        int totalTasks,
        int completedTasks,
        int inProgressTasks,
        LocalDateTime joinedAt) {}
