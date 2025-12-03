package br.com.rafaelvieira.taskmanagement.web.controller;

import br.com.rafaelvieira.taskmanagement.repository.SquadMemberRepository;
import br.com.rafaelvieira.taskmanagement.repository.UserBadgeRepository;
import br.com.rafaelvieira.taskmanagement.repository.UserMonthlyBadgeRepository;
import br.com.rafaelvieira.taskmanagement.repository.UserScoreRepository;
import br.com.rafaelvieira.taskmanagement.service.MonthlyBadgeService;
import br.com.rafaelvieira.taskmanagement.service.UserService;
import br.com.rafaelvieira.taskmanagement.web.dto.UserProfileForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final UserScoreRepository userScoreRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserMonthlyBadgeRepository userMonthlyBadgeRepository;
    private final SquadMemberRepository squadMemberRepository;
    private final MonthlyBadgeService monthlyBadgeService;

    @GetMapping("/profile")
    public String manageProfile(Model model) {
        var user = userService.getCurrentUser();
        model.addAttribute("user", user);
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", userService.loadProfile());
        }
        model.addAttribute(
                "skills",
                extractTags(user.getProfile() != null ? user.getProfile().getSkills() : null));
        model.addAttribute(
                "softSkills",
                extractTags(user.getProfile() != null ? user.getProfile().getSoftSkills() : null));
        return "profile/edit";
    }

    @GetMapping("/profile/edit")
    public String legacyEditRedirect() {
        return "redirect:/profile";
    }

    @GetMapping("/profile/view")
    public String viewProfile(Model model) {
        var user = userService.getCurrentUser();
        model.addAttribute("user", user);

        // Load gamification data
        var userScore = userScoreRepository.findByUser(user).orElse(null);
        var userBadges = userBadgeRepository.findByUser(user);
        var mySquads = squadMemberRepository.findByUser(user);

        model.addAttribute("userScore", userScore);
        model.addAttribute("userBadges", userBadges);
        model.addAttribute("mySquads", mySquads);

        // Load monthly badge data
        var currentMonthlyBadge = monthlyBadgeService.getCurrentMonthBadge(user);
        var monthlyBadgeStats = monthlyBadgeService.getUserStats(user);
        var recentMonthlyBadges =
                userMonthlyBadgeRepository.findByUserOrderByReferenceYearDescReferenceMonthDesc(
                        user);

        model.addAttribute("currentMonthlyBadge", currentMonthlyBadge);
        model.addAttribute("monthlyBadgeStats", monthlyBadgeStats);
        model.addAttribute("recentMonthlyBadges", recentMonthlyBadges.stream().limit(6).toList());

        model.addAttribute(
                "skills",
                extractTags(user.getProfile() != null ? user.getProfile().getSkills() : null));
        model.addAttribute(
                "softSkills",
                extractTags(user.getProfile() != null ? user.getProfile().getSoftSkills() : null));

        return "profile/view";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @Valid @ModelAttribute("profileForm") UserProfileForm form,
            BindingResult result,
            RedirectAttributes ra,
            Model model) {
        if (result.hasErrors()) {
            model.addAttribute("user", userService.getCurrentUser());
            return "profile/edit";
        }
        userService.updateProfile(form);
        ra.addFlashAttribute("successMessage", "Perfil atualizado com sucesso!");
        return "redirect:/profile";
    }

    private java.util.List<String> extractTags(String csv) {
        if (csv == null || csv.isBlank()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
