package br.com.rafaelvieira.taskmanagement.domain.records;

import java.time.LocalDateTime;

public record DashboardUpdateMessage(
        String type,
        Long totalTodo,
        Long totalInProgress,
        Long totalDone,
        Long totalCancelled,
        Long countLow,
        Long countMedium,
        Long countHigh,
        Long countUrgent,
        LocalDateTime timestamp) {

    public static DashboardUpdateMessage create(
            Long totalTodo,
            Long totalInProgress,
            Long totalDone,
            Long totalCancelled,
            Long countLow,
            Long countMedium,
            Long countHigh,
            Long countUrgent) {
        return new DashboardUpdateMessage(
                "DASHBOARD_UPDATE",
                totalTodo,
                totalInProgress,
                totalDone,
                totalCancelled,
                countLow,
                countMedium,
                countHigh,
                countUrgent,
                LocalDateTime.now());
    }
}
