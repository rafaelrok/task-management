package br.com.rafaelvieira.taskmanagement.controller;

import br.com.rafaelvieira.taskmanagement.domain.model.SquadInvite;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.exception.ForbiddenException;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.exception.UnauthorizedException;
import br.com.rafaelvieira.taskmanagement.service.SquadService;
import br.com.rafaelvieira.taskmanagement.service.UserService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/squads/invites")
@RequiredArgsConstructor
@Slf4j
public class SquadInviteController {

    private final SquadService squadService;
    private final UserService userService;

    @GetMapping
    public String listInvites(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        List<SquadInvite> invites = squadService.getPendingInvites(user);
        List<SquadInvite> inviteHistory = squadService.getInviteHistory(user);
        model.addAttribute("invites", invites);
        model.addAttribute("inviteHistory", inviteHistory);
        model.addAttribute("currentUser", user);
        model.addAttribute("activePage", "invites");
        return "squads/invites";
    }

    @PostMapping("/{inviteId}/accept")
    public String acceptInvite(
            @PathVariable("inviteId") Long inviteId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(principal.getName());
            squadService.respondToInvite(inviteId, true, user);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Convite aceito com sucesso! Você agora faz parte da squad.");
        } catch (ResourceNotFoundException e) {
            log.warn("Invite not found: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Convite não encontrado.");
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized to accept invite: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Este convite não é para você.");
        } catch (IllegalStateException e) {
            log.warn("Invalid invite state: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (ForbiddenException e) {
            log.error("Error accepting invite", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Erro ao aceitar convite: " + e.getMessage());
        }
        return "redirect:/squads/invites";
    }

    @PostMapping("/{inviteId}/reject")
    public String rejectInvite(
            @PathVariable("inviteId") Long inviteId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(principal.getName());
            squadService.respondToInvite(inviteId, false, user);
            redirectAttributes.addFlashAttribute("successMessage", "Convite recusado.");
        } catch (ResourceNotFoundException e) {
            log.warn("Invite not found: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Convite não encontrado.");
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized to reject invite: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Este convite não é para você.");
        } catch (ForbiddenException e) {
            log.error("Error rejecting invite", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Erro ao recusar convite: " + e.getMessage());
        }
        return "redirect:/squads/invites";
    }
}
