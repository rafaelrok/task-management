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

    private static boolean isAuthenticated() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken;
    }

    @ModelAttribute("overdueCount")
    public long overdueCount() {
        if (isAuthenticated()) {
            return 0;
        }
        return notificationService.countOverdueForCurrentUser();
    }

    @ModelAttribute("nearDueCount")
    public long nearDueCount() {
        if (isAuthenticated()) {
            return 0;
        }
        return notificationService.countNearDueForCurrentUser();
    }
}
