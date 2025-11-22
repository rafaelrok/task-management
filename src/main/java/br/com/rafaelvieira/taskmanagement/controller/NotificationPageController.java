package br.com.rafaelvieira.taskmanagement.controller;

import br.com.rafaelvieira.taskmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationPageController {

    private final NotificationService notificationService;

    @GetMapping
    public String index(Model model, @PageableDefault(size = 20) Pageable pageable) {
        var notifications = notificationService.findAllForCurrentUser(pageable);
        model.addAttribute("notifications", notifications);
        return "notifications/index";
    }
}
