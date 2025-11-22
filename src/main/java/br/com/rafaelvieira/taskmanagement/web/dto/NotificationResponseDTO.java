package br.com.rafaelvieira.taskmanagement.web.dto;

import br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationResponseDTO {
    Long id;
    String title;
    String message;
    NotificationType type;
    Long taskId;
    boolean read;
    LocalDateTime createdAt;
}
