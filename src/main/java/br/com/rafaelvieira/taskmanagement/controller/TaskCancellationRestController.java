package br.com.rafaelvieira.taskmanagement.controller;

import br.com.rafaelvieira.taskmanagement.domain.model.TaskCancelRequest;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.service.TaskCancellationService;
import br.com.rafaelvieira.taskmanagement.service.UserService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskCancellationRestController {

    private final TaskCancellationService cancellationService;
    private final UserService userService;

    /** Request task cancellation */
    @PostMapping("/tasks/{taskId}/request-cancel")
    public ResponseEntity<@NotNull Void> requestCancellation(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> payload,
            Principal principal) {
        User user = userService.findByUsername(principal.getName());
        String reason = payload.get("reason");

        if (reason == null || reason.trim().length() < 10) {
            return ResponseEntity.badRequest().build();
        }

        cancellationService.requestCancellation(taskId, user, reason);
        return ResponseEntity.ok().build();
    }

    /** Approve cancellation request (LEAD or ADMIN only) */
    @PostMapping("/cancel-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('LEAD', 'ADMIN')")
    public ResponseEntity<@NotNull Void> approveCancellation(
            @PathVariable Long id, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        cancellationService.approveCancellation(id, user);
        return ResponseEntity.ok().build();
    }

    /** Reject cancellation request (LEAD or ADMIN only) */
    @PostMapping("/cancel-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('LEAD', 'ADMIN')")
    public ResponseEntity<@NotNull Void> rejectCancellation(
            @PathVariable Long id, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        cancellationService.rejectCancellation(id, user);
        return ResponseEntity.ok().build();
    }

    /** Get pending cancellation requests for current LEAD */
    @GetMapping("/cancel-requests/pending")
    @PreAuthorize("hasAnyRole('LEAD', 'ADMIN')")
    public ResponseEntity<@NotNull List<TaskCancelRequest>> getPendingRequests(
            Principal principal) {
        User user = userService.findByUsername(principal.getName());
        List<TaskCancelRequest> requests = cancellationService.getPendingRequestsForLead(user);
        return ResponseEntity.ok(requests);
    }
}
