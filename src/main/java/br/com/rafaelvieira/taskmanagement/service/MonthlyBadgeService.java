package br.com.rafaelvieira.taskmanagement.service;

import br.com.rafaelvieira.taskmanagement.domain.enums.BadgeLevel;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.domain.model.UserMonthlyBadge;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.repository.TaskRepository;
import br.com.rafaelvieira.taskmanagement.repository.UserMonthlyBadgeRepository;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serviço para gerenciar selos mensais de usuários baseados na produtividade em tasks de squad */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonthlyBadgeService {

    private final UserMonthlyBadgeRepository monthlyBadgeRepository;
    private final TaskRepository taskRepository;
    private final GamificationWebSocketService webSocketService;

    /**
     * Registra a conclusão de uma task de squad para o usuário Atualiza o contador do mês corrente
     * e recalcula o nível do badge
     */
    @Transactional
    public UserMonthlyBadge registerSquadTaskCompletion(User user) {
        YearMonth currentMonth = YearMonth.now();

        UserMonthlyBadge badge = getOrCreateCurrentMonthBadge(user, currentMonth);
        BadgeLevel previousLevel = badge.getLevel();

        badge.incrementTasksCompleted();

        UserMonthlyBadge savedBadge = monthlyBadgeRepository.save(badge);

        // Notifica se houve mudança de nível
        if (savedBadge.getLevel() != previousLevel) {
            log.info(
                    "User {} promoted to {} level with {} tasks",
                    user.getUsername(),
                    savedBadge.getLevel(),
                    savedBadge.getTasksCompletedInSquads());
            notifyLevelChange(user, savedBadge, previousLevel);
        }

        return savedBadge;
    }

    /** Obtém ou cria o badge do mês corrente para um usuário */
    @Transactional
    public UserMonthlyBadge getOrCreateCurrentMonthBadge(User user, YearMonth month) {
        return monthlyBadgeRepository
                .findByUserAndReferenceYearAndReferenceMonth(
                        user, month.getYear(), month.getMonthValue())
                .orElseGet(
                        () -> {
                            UserMonthlyBadge newBadge =
                                    UserMonthlyBadge.builder()
                                            .user(user)
                                            .referenceYear(month.getYear())
                                            .referenceMonth(month.getMonthValue())
                                            .isCurrentMonth(month.equals(YearMonth.now()))
                                            .build();
                            return monthlyBadgeRepository.save(newBadge);
                        });
    }

    /** Retorna o badge atual (mais recente) do usuário */
    public Optional<UserMonthlyBadge> getCurrentBadge(User user) {
        return monthlyBadgeRepository.findLatestBadgeByUserId(user.getId());
    }

    /** Retorna o badge do mês corrente do usuário */
    public UserMonthlyBadge getCurrentMonthBadge(User user) {
        YearMonth currentMonth = YearMonth.now();
        return getOrCreateCurrentMonthBadge(user, currentMonth);
    }

    /** Retorna o histórico de badges do usuário */
    public List<UserMonthlyBadge> getBadgeHistory(User user) {
        return monthlyBadgeRepository.findAllByUserOrdered(user);
    }

    /** Retorna o nível atual do usuário (do mês mais recente) */
    public BadgeLevel getCurrentLevel(User user) {
        return getCurrentBadge(user).map(UserMonthlyBadge::getLevel).orElse(BadgeLevel.NONE);
    }

    /** Recalcula o badge de um usuário para um mês específico baseado nas tasks reais concluídas */
    @Transactional
    public UserMonthlyBadge recalculateBadge(User user, YearMonth month) {
        Long tasksCompleted =
                taskRepository.countSquadTasksCompletedInMonth(
                        user, month.getYear(), month.getMonthValue());

        UserMonthlyBadge badge = getOrCreateCurrentMonthBadge(user, month);
        badge.setTasksCompletedInSquads(tasksCompleted.intValue());
        badge.recalculateLevel();

        return monthlyBadgeRepository.save(badge);
    }

    /** Job agendado para processar a virada de mês Executa no primeiro dia de cada mês às 00:01 */
    @Scheduled(cron = "0 1 0 1 * *")
    @Transactional
    public void processMonthTransition() {
        log.info("Processing month transition for monthly badges...");

        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        YearMonth currentMonth = YearMonth.now();

        // Marca todos os badges do mês anterior como não-corrente
        List<UserMonthlyBadge> currentBadges = monthlyBadgeRepository.findAllCurrentMonth();
        for (UserMonthlyBadge badge : currentBadges) {
            badge.setIsCurrentMonth(false);
            monthlyBadgeRepository.save(badge);

            // Cria badge para o novo mês se ainda não existe
            getOrCreateCurrentMonthBadge(badge.getUser(), currentMonth);
        }

        log.info("Month transition completed. {} badges processed.", currentBadges.size());
    }

    /** Retorna o ranking mensal global */
    public List<UserMonthlyBadge> getMonthlyRanking(YearMonth month) {
        return monthlyBadgeRepository.findMonthlyRanking(month.getYear(), month.getMonthValue());
    }

    /** Retorna o ranking do mês atual */
    public List<UserMonthlyBadge> getCurrentMonthRanking() {
        return getMonthlyRanking(YearMonth.now());
    }

    /** Verifica se o usuário é elegível para selos (participa de pelo menos um squad) */
    public boolean isEligibleForBadges(User user) {
        return !taskRepository.findSquadTasksForMember(user).isEmpty()
                || getCurrentBadge(user).isPresent();
    }

    /** Notifica o usuário sobre mudança de nível */
    private void notifyLevelChange(User user, UserMonthlyBadge badge, BadgeLevel previousLevel) {
        String message;
        if (badge.getLevel().ordinal() > previousLevel.ordinal()) {
            message =
                    String.format(
                            "Parabéns! Você subiu para o nível %s! 🎉",
                            badge.getLevel().getDisplayName());
        } else {
            message =
                    String.format(
                            "Seu nível mudou para %s. Continue completando tasks!",
                            badge.getLevel().getDisplayName());
        }

        try {
            webSocketService.notifyPointsEarned(user, 0, message);
        } catch (ResourceNotFoundException e) {
            log.warn("Failed to send level change notification: {}", e.getMessage());
        }
    }

    /** Retorna estatísticas de badges de um usuário */
    public MonthlyBadgeStats getUserStats(User user) {
        List<UserMonthlyBadge> history = getBadgeHistory(user);
        long diamondCount = monthlyBadgeRepository.countDiamondBadgesByUser(user);

        int totalTasks =
                history.stream().mapToInt(UserMonthlyBadge::getTasksCompletedInSquads).sum();

        BadgeLevel highestLevel =
                history.stream()
                        .map(UserMonthlyBadge::getLevel)
                        .max(Enum::compareTo)
                        .orElse(BadgeLevel.NONE);

        return new MonthlyBadgeStats(
                getCurrentLevel(user),
                highestLevel,
                (int) diamondCount,
                totalTasks,
                history.size());
    }

    /** Record para estatísticas de badges */
    public record MonthlyBadgeStats(
            BadgeLevel currentLevel,
            BadgeLevel highestLevel,
            int diamondCount,
            int totalTasksCompleted,
            int monthsTracked) {}
}
