package br.com.rafaelvieira.taskmanagement.service;

import br.com.rafaelvieira.taskmanagement.domain.entity.Notification;
import br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationService {
    long countOverdueForCurrentUser();

    long countNearDueForCurrentUser();

    Notification createNotification(
            String title, String message, NotificationType type, Long taskId, User user);

    /**
     * Cria uma notificação sticky (persistente) que requer ação manual do usuário. Usada para
     * alertas críticos como PENDING e OVERDUE.
     */
    Notification createStickyNotification(
            String title, String message, NotificationType type, Long taskId, User user);

    Page<Notification> findAllForCurrentUser(Pageable pageable);

    List<Notification> findUnreadForCurrentUser();

    /** Retorna todas as notificações sticky não lidas do usuário atual. */
    List<Notification> findStickyUnreadForCurrentUser();

    long countUnreadForCurrentUser();

    void markAsRead(Long notificationId);

    void markAllAsRead();

    SseEmitter subscribe(Long userId);
}
