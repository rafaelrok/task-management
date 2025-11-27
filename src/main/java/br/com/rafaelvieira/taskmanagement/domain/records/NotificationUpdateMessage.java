package br.com.rafaelvieira.taskmanagement.domain.records;

import java.time.LocalDateTime;

public record NotificationUpdateMessage(
        String type, Long notificationId, String title, String message, LocalDateTime timestamp) {

    public static NotificationUpdateMessage create(
            Long notificationId, String title, String message) {
        return new NotificationUpdateMessage(
                "NOTIFICATION_UPDATE", notificationId, title, message, LocalDateTime.now());
    }
}
