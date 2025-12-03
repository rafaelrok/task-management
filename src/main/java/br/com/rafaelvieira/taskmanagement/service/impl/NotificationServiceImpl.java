package br.com.rafaelvieira.taskmanagement.service.impl;

import br.com.rafaelvieira.taskmanagement.domain.entity.Notification;
import br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.exception.ForbiddenException;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.exception.UnauthorizedException;
import br.com.rafaelvieira.taskmanagement.repository.NotificationRepository;
import br.com.rafaelvieira.taskmanagement.repository.TaskRepository;
import br.com.rafaelvieira.taskmanagement.service.NotificationService;
import br.com.rafaelvieira.taskmanagement.service.UserService;
import br.com.rafaelvieira.taskmanagement.web.dto.NotificationResponseDTO;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final TaskRepository taskRepository;
    private final UserService userService;
    private final NotificationRepository notificationRepository;
    private final br.com.rafaelvieira.taskmanagement.service.WebSocketService webSocketService;
    private final java.util.Map<Long, SseEmitter> emitters =
            new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public long countOverdueForCurrentUser() {
        var user = userService.getCurrentUser();
        return taskRepository.findOverdueTasks(LocalDateTime.now()).stream()
                .filter(
                        t ->
                                t.getAssignedUser() != null
                                        && t.getAssignedUser().getId().equals(user.getId()))
                .count();
    }

    @Override
    public long countNearDueForCurrentUser() {
        var user = userService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime window = now.plusHours(48);
        return taskRepository.findAll().stream()
                .filter(
                        t ->
                                t.getAssignedUser() != null
                                        && t.getAssignedUser().getId().equals(user.getId()))
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .filter(
                        t ->
                                t.getDueDate() != null
                                        && !t.getDueDate().isBefore(now)
                                        && t.getDueDate().isBefore(window))
                .count();
    }

    @Override
    @Transactional
    public Notification createNotification(
            String title, String message, NotificationType type, Long taskId, User user) {
        return createNotificationInternal(title, message, type, taskId, user, false);
    }

    @Override
    @Transactional
    public Notification createStickyNotification(
            String title, String message, NotificationType type, Long taskId, User user) {
        return createNotificationInternal(title, message, type, taskId, user, true);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        var currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        notificationRepository.markAllAsRead(currentUser.getId());
    }

    @Override
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(userId, emitter);
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));
        return emitter;
    }

    private void sendSseNotification(Long userId, Notification notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                NotificationResponseDTO dto =
                        NotificationResponseDTO.builder()
                                .id(notification.getId())
                                .title(notification.getTitle())
                                .message(notification.getMessage())
                                .type(notification.getType())
                                .taskId(notification.getTaskId())
                                .read(notification.isRead())
                                .sticky(notification.isSticky())
                                .createdAt(notification.getCreatedAt())
                                .build();
                emitter.send(SseEmitter.event().name("notification").data(dto));
            } catch (IOException e) {
                emitters.remove(userId);
                log.warn(
                        "Failed to send SSE notification {} for user {}: {}",
                        notification.getId(),
                        userId,
                        e.getMessage());
            }
        }
    }

    private Notification createNotificationInternal(
            String title,
            String message,
            NotificationType type,
            Long taskId,
            User user,
            boolean sticky) {
        if (user == null) {
            log.debug("Skipping notification creation for task {} because user is null", taskId);
            return null; // evita constraint violation
        }

        // Deduplication: Prevent duplicate unread notifications for the same task and
        // type
        // Especially for sticky notifications (OVERDUE, PENDING, TODO)
        if (sticky
                || type == NotificationType.TASK_OVERDUE
                || type == NotificationType.TASK_PENDING) {
            boolean exists =
                    notificationRepository.existsByUserAndTaskIdAndTypeAndReadFalse(
                            user, taskId, type);
            if (exists) {
                log.debug("Skipping duplicate notification for task {} type {}", taskId, type);
                return null;
            }
        }

        var notification =
                Notification.builder()
                        .title(title)
                        .message(message)
                        .type(type)
                        .taskId(taskId)
                        .user(user)
                        .read(false)
                        .sticky(sticky)
                        .createdAt(LocalDateTime.now())
                        .build();

        var saved = notificationRepository.save(notification);
        sendSseNotification(user.getId(), saved);

        // Send via WebSocket
        try {
            webSocketService.sendNotificationToUser(
                    user.getUsername(),
                    br.com.rafaelvieira.taskmanagement.domain.records.NotificationUpdateMessage
                            .create(
                                    saved.getId(),
                                    saved.getTitle(),
                                    saved.getMessage(),
                                    saved.getType().name(),
                                    saved.getTaskId(),
                                    saved.isSticky()));
        } catch (ResourceNotFoundException e) {
            log.error("Failed to send WebSocket notification", e);
        }

        return saved;
    }

    @Override
    public Page<@NotNull Notification> findAllForCurrentUser(Pageable pageable) {
        var currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            log.warn("Attempt to list notifications without authentication");
            throw new UnauthorizedException("User not authenticated");
        }
        return notificationRepository.findByUserOrderByCreatedAtDesc(currentUser, pageable);
    }

    @Override
    public List<Notification> findUnreadForCurrentUser() {
        var currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(currentUser);
    }

    @Override
    public List<Notification> findStickyUnreadForCurrentUser() {
        var currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        return notificationRepository.findByUserAndReadFalseAndStickyTrueOrderByCreatedAtDesc(
                currentUser);
    }

    @Override
    public long countUnreadForCurrentUser() {
        var currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        return notificationRepository.countByUserAndReadFalse(currentUser);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        var notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        var currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            log.warn(
                    "User {} attempted to access notification {} of user {}",
                    currentUser.getId(),
                    notificationId,
                    notification.getUser().getId());
            throw new ForbiddenException("Unauthorized access to notification");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }
}
