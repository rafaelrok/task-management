package br.com.rafaelvieira.taskmanagement.domain.records;

import java.time.LocalDateTime;

public record NotificationUpdateMessage(
        Long id,
        String title,
        String message,
        String type,
        Long taskId,
        boolean sticky,
        LocalDateTime createdAt) {

    public static NotificationUpdateMessage create(
            Long id, String title, String message, String type, Long taskId, boolean sticky) {
        return new NotificationUpdateMessage(
                id, title, message, type, taskId, sticky, LocalDateTime.now());
    }
}
