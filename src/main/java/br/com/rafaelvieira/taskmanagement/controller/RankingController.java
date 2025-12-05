package br.com.rafaelvieira.taskmanagement.controller;

import br.com.rafaelvieira.taskmanagement.domain.model.Squad;
import br.com.rafaelvieira.taskmanagement.domain.model.SquadMember;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.domain.model.UserScore;
import br.com.rafaelvieira.taskmanagement.exception.ForbiddenException;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.repository.SquadMemberRepository;
import br.com.rafaelvieira.taskmanagement.repository.SquadRepository;
import br.com.rafaelvieira.taskmanagement.repository.UserScoreRepository;
import br.com.rafaelvieira.taskmanagement.service.UserService;
import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ranking")
@RequiredArgsConstructor
@Slf4j
public class RankingController {

    private final UserScoreRepository userScoreRepository;
    private final SquadRepository squadRepository;
    private final SquadMemberRepository squadMemberRepository;
    private final UserService userService;

    @GetMapping
    public String globalRanking(Model model, Principal principal) {
        User currentUser = userService.findByUsername(principal.getName());

        // Get all user scores sorted by points
        List<UserScore> rankings =
                userScoreRepository.findAll(Sort.by(Sort.Direction.DESC, "totalPoints"));

        // Find current user position and score
        int userPosition = 0;
        UserScore currentUserScore = null;
        for (int i = 0; i < rankings.size(); i++) {
            if (rankings.get(i).getUser().getId().equals(currentUser.getId())) {
                userPosition = i + 1;
                currentUserScore = rankings.get(i);
                break;
            }
        }

        model.addAttribute("rankings", rankings);
        model.addAttribute("userPosition", userPosition);
        model.addAttribute("currentUserScore", currentUserScore);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activePage", "ranking");

        return "ranking/global";
    }

    @GetMapping("/squad/{squadId}")
    public String squadRanking(
            @PathVariable(name = "squadId") Long squadId,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.findByUsername(principal.getName());
            Squad squad =
                    squadRepository
                            .findById(squadId)
                            .orElseThrow(() -> new ResourceNotFoundException("Squad not found"));

            List<SquadMember> members = squadMemberRepository.findBySquad(squad);

            // Get scores for squad members and sort
            List<UserScore> rankings =
                    members.stream()
                            .map(SquadMember::getUser)
                            .map(
                                    user ->
                                            userScoreRepository
                                                    .findByUser(user)
                                                    .orElse(UserScore.builder().user(user).build()))
                            .sorted(Comparator.comparing(UserScore::getTotalPoints).reversed())
                            .collect(Collectors.toList());

            int userPosition = 0;
            UserScore currentUserScore = null;
            for (int i = 0; i < rankings.size(); i++) {
                if (rankings.get(i).getUser().getId().equals(currentUser.getId())) {
                    userPosition = i + 1;
                    currentUserScore = rankings.get(i);
                    break;
                }
            }

            model.addAttribute("squad", squad);
            model.addAttribute("rankings", rankings);
            model.addAttribute("userPosition", userPosition);
            model.addAttribute("currentUserScore", currentUserScore);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("activePage", "ranking");

            return "ranking/squad";
        } catch (ResourceNotFoundException e) {
            log.warn("Squad not found: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/ranking";
        } catch (ForbiddenException e) {
            log.error("Error loading squad ranking", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Erro ao carregar ranking da squad");
            return "redirect:/ranking";
        }
    }
}
