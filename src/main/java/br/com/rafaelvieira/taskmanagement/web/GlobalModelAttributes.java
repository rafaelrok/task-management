package br.com.rafaelvieira.taskmanagement.web;

import br.com.rafaelvieira.taskmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@Component
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final NotificationService notificationService;
    private final br.com.rafaelvieira.taskmanagement.service.UserService userService;

    private static boolean isNotAuthenticated() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken;
    }

    @ModelAttribute("currentUser")
    public br.com.rafaelvieira.taskmanagement.domain.model.User currentUser() {
        if (isNotAuthenticated()) {
            return null;
        }
        return userService.getCurrentUser();
    }

    @ModelAttribute("overdueCount")
    public long overdueCount() {
        if (isNotAuthenticated()) {
            return 0;
        }
        return notificationService.countOverdueForCurrentUser();
    }

    @ModelAttribute("nearDueCount")
    public long nearDueCount() {
        if (isNotAuthenticated()) {
            return 0;
        }
        return notificationService.countNearDueForCurrentUser();
    }
}
