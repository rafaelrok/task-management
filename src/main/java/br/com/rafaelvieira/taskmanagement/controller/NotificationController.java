package br.com.rafaelvieira.taskmanagement.controller;

import br.com.rafaelvieira.taskmanagement.service.NotificationService;
import br.com.rafaelvieira.taskmanagement.service.UserService;
import br.com.rafaelvieira.taskmanagement.web.dto.NotificationResponseDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(notificationService.subscribe(currentUser.getId()));
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponseDTO>> getNotifications(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(name = "unreadOnly", required = false, defaultValue = "false")
                    boolean unreadOnly) {
        var page =
                (unreadOnly
                                ? notificationService.findAllUnreadForCurrentUser(pageable)
                                : notificationService.findAllForCurrentUser(pageable))
                        .map(
                                n ->
                                        NotificationResponseDTO.builder()
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
    public ResponseEntity<List<NotificationResponseDTO>> getStickyNotifications() {
        var notifications =
                notificationService.findStickyUnreadForCurrentUser().stream()
                        .map(
                                n ->
                                        NotificationResponseDTO.builder()
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
