package br.com.rafaelvieira.taskmanagement.controller;

import br.com.rafaelvieira.taskmanagement.domain.model.TaskCancelRequest;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.repository.TaskCancelRequestRepository;
import br.com.rafaelvieira.taskmanagement.service.UserService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cancel-requests")
@RequiredArgsConstructor
public class TaskCancellationViewController {

    private final TaskCancelRequestRepository cancelRequestRepository;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('LEAD', 'ADMIN')")
    public String listPendingRequests(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        List<TaskCancelRequest> pendingRequests =
                cancelRequestRepository.findByLeadToApproveAndStatusOrderByCreatedAtDesc(
                        user,
                        br.com.rafaelvieira.taskmanagement.domain.enums.TaskCancelRequestStatus
                                .PENDING);

        model.addAttribute("requests", pendingRequests);
        model.addAttribute("currentUser", user);
        return "cancellation/list";
    }
}
