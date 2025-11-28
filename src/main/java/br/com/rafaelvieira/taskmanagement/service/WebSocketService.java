package br.com.rafaelvieira.taskmanagement.service;

import br.com.rafaelvieira.taskmanagement.domain.records.DashboardUpdateMessage;
import br.com.rafaelvieira.taskmanagement.domain.records.NotificationUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /** Envia atualização de dashboard para todos os clientes conectados */
    public void sendDashboardUpdate(DashboardUpdateMessage message) {
        try {
            messagingTemplate.convertAndSend("/topic/dashboard", message);
            log.debug("Dashboard update sent: {}", message);
        } catch (MessagingException e) {
            log.error("Error sending dashboard update via WebSocket", e);
        }
    }

    /** Envia notificação para um utilizador específico */
    public void sendNotificationToUser(String username, NotificationUpdateMessage message) {
        try {
            messagingTemplate.convertAndSendToUser(username, "/queue/notifications", message);
            log.debug("Notification sent to user {}: {}", username, message);
        } catch (MessagingException e) {
            log.error("Error sending notification to user {} via WebSocket", username, e);
        }
    }

    /** Envia notificação para todos os utilizadores */
    public void sendNotificationBroadcast(NotificationUpdateMessage message) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications", message);
            log.debug("Notification broadcast sent: {}", message);
        } catch (MessagingException e) {
            log.error("Error sending notification broadcast via WebSocket", e);
        }
    }
}
