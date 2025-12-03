package br.com.rafaelvieira.taskmanagement.service;

import br.com.rafaelvieira.taskmanagement.domain.model.Badge;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.domain.model.UserBadge;
import br.com.rafaelvieira.taskmanagement.domain.model.UserScore;
import br.com.rafaelvieira.taskmanagement.repository.BadgeRepository;
import br.com.rafaelvieira.taskmanagement.repository.UserBadgeRepository;
import br.com.rafaelvieira.taskmanagement.repository.UserScoreRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationService {

    private final UserScoreRepository userScoreRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final GamificationWebSocketService webSocketService;

    @Transactional
    public void registerTaskCompletion(Task task) {
        if (task.getSquad() == null) {
            return; // Only squad tasks count
        }

        User user = task.getAssignedUser();
        if (user == null) {
            return;
        }

        UserScore score =
                userScoreRepository
                        .findByUser(user)
                        .orElseGet(() -> UserScore.builder().user(user).build());

        boolean isEarly =
                task.getDueDate() != null
                        && task.getCompletedAt() != null
                        && task.getCompletedAt().isBefore(task.getDueDate());

        int pointsToAdd = isEarly ? 20 : 10;
        score.setTotalPoints(score.getTotalPoints() + pointsToAdd);
        score.setTotalTasksCompletedInSquads(score.getTotalTasksCompletedInSquads() + 1);
        if (isEarly) {
            score.setTotalTasksCompletedEarly(score.getTotalTasksCompletedEarly() + 1);
        }

        userScoreRepository.save(score);
        webSocketService.notifyPointsEarned(
                user,
                pointsToAdd,
                isEarly ? "Tarefa concluída antecipadamente!" : "Tarefa concluída!");
        checkAndAwardBadges(user, score);
    }

    private void checkAndAwardBadges(User user, UserScore score) {
        if (score.getTotalTasksCompletedInSquads() >= 10) {
            awardBadge(user, "SQUAD_COMMITED_10");
        }
        if (score.getTotalTasksCompletedEarly() >= 5) {
            awardBadge(user, "EARLY_FINISHER_5");
        }
        if (score.getTotalTasksCompletedEarly() >= 20) {
            awardBadge(user, "EARLY_FINISHER_20");
        }
    }

    private void awardBadge(User user, String badgeCode) {
        Optional<Badge> badgeOpt = badgeRepository.findByCode(badgeCode);
        if (badgeOpt.isPresent()) {
            Badge badge = badgeOpt.get();
            if (!userBadgeRepository.existsByUserAndBadge(user, badge)) {
                UserBadge userBadge = UserBadge.builder().user(user).badge(badge).build();
                UserBadge savedBadge = userBadgeRepository.save(userBadge);
                webSocketService.notifyBadgeEarned(user, savedBadge);
                log.info("Awarded badge {} to user {}", badgeCode, user.getUsername());
            }
        } else {
            // Create badge if not exists (for simplicity in this demo, usually seeded)
            Badge newBadge =
                    Badge.builder()
                            .code(badgeCode)
                            .name(formatBadgeName(badgeCode))
                            .description("Awarded for " + badgeCode)
                            .iconClass("fa-solid fa-medal")
                            .build();
            badgeRepository.save(newBadge);
            awardBadge(user, badgeCode); // Retry
        }
    }

    private String formatBadgeName(String code) {
        return code.replace("_", " ");
    }
}
