package br.com.rafaelvieira.taskmanagement.controller;

import br.com.rafaelvieira.taskmanagement.domain.enums.Role;
import br.com.rafaelvieira.taskmanagement.domain.enums.SquadType;
import br.com.rafaelvieira.taskmanagement.domain.model.Squad;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.exception.ForbiddenException;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.exception.SquadValidationException;
import br.com.rafaelvieira.taskmanagement.exception.UnauthorizedException;
import br.com.rafaelvieira.taskmanagement.service.SquadService;
import br.com.rafaelvieira.taskmanagement.service.SquadService.SquadCreateDTO;
import br.com.rafaelvieira.taskmanagement.service.SquadService.SquadDashboardDTO;
import br.com.rafaelvieira.taskmanagement.service.SquadService.SquadUpdateDTO;
import br.com.rafaelvieira.taskmanagement.service.TaskService;
import br.com.rafaelvieira.taskmanagement.service.UserService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/squads")
@RequiredArgsConstructor
@Slf4j
public class SquadController {

    private final SquadService squadService;
    private final TaskService taskService;
    private final UserService userService;

    /** Lista squads para MEMBER (apenas squads que participa) */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public String mySquads(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        List<Squad> squads = squadService.getSquadsByMember(user);

        model.addAttribute("squads", squads);
        model.addAttribute("squadTypes", SquadType.values());
        model.addAttribute("activePage", "my-squads");
        model.addAttribute("viewMode", "member");
        return "squads/my-squads";
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listSquads(
            @RequestParam(name = "type", required = false) SquadType type,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "search", required = false) String search,
            Model model,
            Principal principal) {
        User user = userService.findByUsername(principal.getName());

        // Redirect MEMBER users to their squads page
        if (user.getRole() == Role.MEMBER) {
            return "redirect:/squads/my";
        }

        List<Squad> squads;
        if (user.getRole() == Role.ADMIN) {
            // Admin pode ver todos os squads com filtros
            squads = squadService.getSquadsWithFilters(type, active, search);
        } else {
            // Lead vê apenas squads que lidera
            if (active != null && !active) {
                squads = squadService.getSquadsByLead(user);
            } else {
                squads = squadService.getActiveSquadsByLead(user);
            }
        }

        // Criar mapa com contagem de membros por squad
        java.util.Map<Long, Long> memberCountMap = new java.util.HashMap<>();
        for (Squad squad : squads) {
            memberCountMap.put(squad.getId(), squadService.getMemberCount(squad.getId()));
        }

        model.addAttribute("squads", squads);
        model.addAttribute("memberCountMap", memberCountMap);
        model.addAttribute("squadTypes", SquadType.values());
        model.addAttribute("filterType", type);
        model.addAttribute("filterActive", active);
        model.addAttribute("filterSearch", search);
        model.addAttribute("activePage", "squads");
        return "squads/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('LEAD', 'ADMIN')")
    public String createForm(Model model) {
        model.addAttribute("squadTypes", SquadType.values());
        model.addAttribute("activePage", "squads");
        return "squads/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('LEAD', 'ADMIN')")
    public String create(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "description") String description,
            @RequestParam(name = "type", required = false) SquadType type,
            @RequestParam(name = "techStack", required = false) String techStack,
            @RequestParam(name = "businessArea", required = false) String businessArea,
            @RequestParam(name = "goal", required = false) String goal,
            @RequestParam(name = "maxMembers", required = false) Integer maxMembers,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(principal.getName());
            SquadCreateDTO dto = new SquadCreateDTO(
                    name, description, type, techStack, businessArea, goal, maxMembers);
            squadService.createSquad(dto, user);
            redirectAttributes.addFlashAttribute("successMessage", "Squad criada com sucesso!");
            return "redirect:/squads";
        } catch (ResourceNotFoundException e) {
            log.error("Error creating squad", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Erro ao criar squad: " + e.getMessage());
            return "redirect:/squads/create";
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public String details(
            @PathVariable(name = "id") Long id,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(principal.getName());
            Squad squad = validateSquadAccess(id, user);

            model.addAttribute("squad", squad);
            model.addAttribute("members", squadService.getSquadMembers(squad));
            model.addAttribute("invites", squadService.getSquadInvites(squad));
            model.addAttribute("squadTypes", SquadType.values());
            model.addAttribute("activePage", "squads");

            return "squads/details";
        } catch (ResourceNotFoundException e) {
            log.warn("Squad not found: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/squads";
        } catch (UnauthorizedException e) {
            log.warn("Access denied: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Acesso negado");
            return "redirect:/squads";
        } catch (ForbiddenException e) {
            log.error("Error loading squad details", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Erro ao carregar detalhes da squad");
            return "redirect:/squads";
        }
    }

    @GetMapping("/{id}/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(
            @PathVariable(name = "id") Long id,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(principal.getName());
            SquadDashboardDTO dashboard = squadService.getSquadDashboard(id, user);

            model.addAttribute("dashboard", dashboard);
            model.addAttribute("squad", dashboard.squad());
            model.addAttribute("tasks", taskService.getTasksBySquadId(id));
            model.addAttribute("currentUser", user);
            model.addAttribute("activePage", "squads");

            return "squads/dashboard";
        } catch (ResourceNotFoundException e) {
            log.warn("Squad not found: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/squads";
        } catch (UnauthorizedException e) {
            log.warn("Access denied: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Acesso negado");
            return "redirect:/squads";
        } catch (ForbiddenException e) {
            log.error("Error loading squad dashboard", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao carregar dashboard");
            return "redirect:/squads";
        } catch (SquadValidationException e) {
            log.error("Unexpected error loading squad dashboard {}", id, e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Erro interno ao carregar dashboard da squad");
            return "redirect:/squads";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('LEAD', 'ADMIN')")
    public String editForm(
            @PathVariable(name = "id") Long id,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(principal.getName());
            Squad squad = validateSquadAccess(id, user);

            model.addAttribute("squad", squad);
            model.addAttribute("memberCount", squadService.getMemberCount(id));
            model.addAttribute("squadTypes", SquadType.values());
            model.addAttribute("activePage", "squads");
            return "squads/edit";
        } catch (ResourceNotFoundException e) {
            log.error("Error loading squad edit form", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/squads";
        }
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('LEAD', 'ADMIN')")
    public String update(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "name") String name,
            @RequestParam(name = "description") String description,
            @RequestParam(name = "type", required = false) SquadType type,
            @RequestParam(name = "techStack", required = false) String techStack,
            @RequestParam(name = "businessArea", required = false) String businessArea,
            @RequestParam(name = "goal", required = false) String goal,
            @RequestParam(name = "maxMembers", required = false) Integer maxMembers,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(principal.getName());
            SquadUpdateDTO dto = new SquadUpdateDTO(
                    name, description, type, techStack, businessArea, goal, maxMembers);
            squadService.updateSquad(id, dto, user);
            redirectAttributes.addFlashAttribute("successMessage", "Squad atualizada com sucesso!");
            return "redirect:/squads/" + id;
        } catch (ResourceNotFoundException e) {
            log.error("Error updating squad", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Erro ao atualizar squad: " + e.getMessage());
            return "redirect:/squads/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('LEAD', 'ADMIN')")
    public String deactivate(
            @PathVariable(name = "id") Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(principal.getName());
            squadService.deactivateSquad(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Squad desativada com sucesso!");
            return "redirect:/squads";
        } catch (ResourceNotFoundException e) {
            log.error("Error deactivating squad", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Erro ao desativar squad: " + e.getMessage());
            return "redirect:/squads/" + id;
        }
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public String activate(
            @PathVariable(name = "id") Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(principal.getName());
            squadService.activateSquad(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Squad reativada com sucesso!");
            return "redirect:/squads/" + id;
        } catch (ResourceNotFoundException e) {
            log.error("Error activating squad", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Erro ao ativar squad: " + e.getMessage());
            return "redirect:/squads";
        }
    }

    @PostMapping("/{id}/invite")
    @PreAuthorize("hasAnyRole('LEAD', 'ADMIN')")
    public String invite(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "email") String email,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(principal.getName());
            squadService.inviteUser(id, email, user);
            redirectAttributes.addFlashAttribute("successMessage", "Convite enviado com sucesso!");
        } catch (ResourceNotFoundException e) {
            log.warn("User not found for invite: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Invalid invite: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (ForbiddenException e) {
            log.error("Error sending invite", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Erro ao enviar convite: " + e.getMessage());
        }
        return "redirect:/squads/" + id;
    }

    @PostMapping("/{id}/members/{userId}/remove")
    @PreAuthorize("hasAnyRole('LEAD', 'ADMIN')")
    public String removeMember(
            @PathVariable(name = "id") Long id,
            @PathVariable(name = "userId") Long userId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(principal.getName());
            squadService.removeMember(id, userId, user);
            redirectAttributes.addFlashAttribute("successMessage", "Membro removido com sucesso!");
        } catch (ResourceNotFoundException e) {
            log.error("Error removing member", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Erro ao remover membro: " + e.getMessage());
        }
        return "redirect:/squads/" + id;
    }

    private Squad validateSquadAccess(Long squadId, User user) {
        Squad squad = squadService
                .getSquadById(squadId)
                .orElseThrow(() -> new ResourceNotFoundException("Squad not found"));

        if (user.getRole() == Role.ADMIN) {
            return squad;
        }

        // LEAD pode acessar squads que lidera
        if (squad.getLead().getId().equals(user.getId())) {
            return squad;
        }

        // MEMBER pode acessar squads onde é membro
        if (user.getRole() == Role.MEMBER) {
            boolean isMember = squadService.isUserMemberOfSquad(user, squad);
            if (isMember) {
                return squad;
            }
        }

        throw new UnauthorizedException("Access denied to this squad");
    }
}
