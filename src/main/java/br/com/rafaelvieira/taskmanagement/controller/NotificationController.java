package br.com.rafaelvieira.taskmanagement.controller;

import br.com.rafaelvieira.taskmanagement.service.NotificationService;
import br.com.rafaelvieira.taskmanagement.service.UserService;
import br.com.rafaelvieira.taskmanagement.web.dto.NotificationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamNotifications() {
        var currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            // Retorna 401 sem body para que o EventSource falhe uma vez e não gere exceção
            // de media
            // type
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(notificationService.subscribe(currentUser.getId()));
    }

    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<NotificationResponseDTO>> getNotifications(
            @PageableDefault(size = 20) Pageable pageable) {
        var page = notificationService
                .findAllForCurrentUser(pageable)
                .map(
                        n -> NotificationResponseDTO.builder()
                                .id(n.getId())
                                .title(n.getTitle())
                                .message(n.getMessage())
                                .type(n.getType())
                                .taskId(n.getTaskId())
                                .read(n.isRead())
                                .sticky(n.isSticky())
                                .createdAt(n.getCreatedAt())
                                .build());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/sticky")
    public ResponseEntity<java.util.List<NotificationResponseDTO>> getStickyNotifications() {
        var notifications = notificationService.findStickyUnreadForCurrentUser().stream()
                .map(
                        n -> NotificationResponseDTO.builder()
                                .id(n.getId())
                                .title(n.getTitle())
                                .message(n.getMessage())
                                .type(n.getType())
                                .taskId(n.getTaskId())
                                .read(n.isRead())
                                .sticky(n.isSticky())
                                .createdAt(n.getCreatedAt())
                                .build())
                .toList();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        return ResponseEntity.ok(notificationService.countUnreadForCurrentUser());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable("id") Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }
}
