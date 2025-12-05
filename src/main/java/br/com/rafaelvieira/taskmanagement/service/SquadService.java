package br.com.rafaelvieira.taskmanagement.service;

import br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType;
import br.com.rafaelvieira.taskmanagement.domain.enums.Role;
import br.com.rafaelvieira.taskmanagement.domain.enums.SquadInviteStatus;
import br.com.rafaelvieira.taskmanagement.domain.enums.SquadType;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.Squad;
import br.com.rafaelvieira.taskmanagement.domain.model.SquadInvite;
import br.com.rafaelvieira.taskmanagement.domain.model.SquadMember;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.domain.records.MemberMetricsDTO;
import br.com.rafaelvieira.taskmanagement.domain.records.SquadCreateDTO;
import br.com.rafaelvieira.taskmanagement.domain.records.SquadDashboardDTO;
import br.com.rafaelvieira.taskmanagement.domain.records.SquadUpdateDTO;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.exception.UnauthorizedException;
import br.com.rafaelvieira.taskmanagement.repository.SquadInviteRepository;
import br.com.rafaelvieira.taskmanagement.repository.SquadMemberRepository;
import br.com.rafaelvieira.taskmanagement.repository.SquadRepository;
import br.com.rafaelvieira.taskmanagement.repository.TaskRepository;
import br.com.rafaelvieira.taskmanagement.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SquadService {

    private final SquadRepository squadRepository;
    private final SquadMemberRepository squadMemberRepository;
    private final SquadInviteRepository squadInviteRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final GamificationWebSocketService webSocketService;
    private final NotificationService notificationService;

    @Transactional
    public Squad createSquad(String name, String description, User lead) {
        if (lead.getRole() != Role.LEAD && lead.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only LEAD or ADMIN can create squads");
        }

        if (squadRepository.existsByNameIgnoreCaseAndActiveTrue(name)) {
            throw new IllegalArgumentException("A squad with this name already exists");
        }

        Squad squad = Squad.builder().name(name).description(description).lead(lead).build();
        squad = squadRepository.save(squad);

        // Add lead as member
        SquadMember member = SquadMember.builder().squad(squad).user(lead).build();
        squadMemberRepository.save(member);

        log.info("Squad '{}' created by user {}", name, lead.getUsername());
        return squad;
    }

    @Transactional
    public Squad createSquad(SquadCreateDTO dto, User lead) {
        if (lead.getRole() != Role.LEAD && lead.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only LEAD or ADMIN can create squads");
        }

        if (squadRepository.existsByNameIgnoreCaseAndActiveTrue(dto.name())) {
            throw new IllegalArgumentException("A squad with this name already exists");
        }

        Squad squad =
                Squad.builder()
                        .name(dto.name())
                        .description(dto.description())
                        .type(dto.type() != null ? dto.type() : SquadType.FULLSTACK)
                        .techStack(dto.techStack())
                        .businessArea(dto.businessArea())
                        .goal(dto.goal())
                        .maxMembers(dto.maxMembers())
                        .lead(lead)
                        .build();
        squad = squadRepository.save(squad);

        // Add lead as member
        SquadMember member = SquadMember.builder().squad(squad).user(lead).build();
        squadMemberRepository.save(member);

        log.info(
                "Squad '{}' (type: {}) created by user {}",
                squad.getName(),
                squad.getType(),
                lead.getUsername());
        return squad;
    }

    @Transactional
    public Squad updateSquad(Long squadId, SquadUpdateDTO dto, User currentUser) {
        Squad squad =
                squadRepository
                        .findById(squadId)
                        .orElseThrow(() -> new ResourceNotFoundException("Squad not found"));

        validateSquadAccess(squad, currentUser);

        if (dto.name() != null && !dto.name().equals(squad.getName())) {
            if (squadRepository.existsByNameIgnoreCaseAndActiveTrue(dto.name())) {
                throw new IllegalArgumentException("A squad with this name already exists");
            }
            squad.setName(dto.name());
        }

        updateIfNotNull(dto.description(), squad::setDescription);
        updateIfNotNull(dto.type(), squad::setType);
        updateIfNotNull(dto.techStack(), squad::setTechStack);
        updateIfNotNull(dto.businessArea(), squad::setBusinessArea);
        updateIfNotNull(dto.goal(), squad::setGoal);
        updateIfNotNull(dto.maxMembers(), squad::setMaxMembers);

        return squadRepository.save(squad);
    }

    @Transactional
    public void deactivateSquad(Long squadId, User currentUser) {
        Squad squad =
                squadRepository
                        .findById(squadId)
                        .orElseThrow(() -> new ResourceNotFoundException("Squad not found"));

        validateSquadAccess(squad, currentUser);

        squad.deactivate();
        squadRepository.save(squad);
        log.info("Squad '{}' deactivated by user {}", squad.getName(), currentUser.getUsername());
    }

    @Transactional
    public void activateSquad(Long squadId, User currentUser) {
        Squad squad =
                squadRepository
                        .findById(squadId)
                        .orElseThrow(() -> new ResourceNotFoundException("Squad not found"));

        if (currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only ADMIN can reactivate squads");
        }

        squad.activate();
        squadRepository.save(squad);
        log.info("Squad '{}' activated by user {}", squad.getName(), currentUser.getUsername());
    }

    // ========== Query Operations ==========

    public List<Squad> getSquadsByLead(User lead) {
        return squadRepository.findByLead(lead);
    }

    public List<Squad> getActiveSquadsByLead(User lead) {
        return squadRepository.findByLeadAndActiveTrue(lead);
    }

    public List<Squad> getAllActiveSquads() {
        return squadRepository.findByActiveTrue();
    }

    public List<Squad> getSquadsForDropdown(User user) {
        if (user.getRole() == Role.MEMBER) {
            return List.of();
        }
        return squadRepository.findByLeadAndActiveTrue(user);
    }

    public List<Squad> getSquadsWithFilters(SquadType type, Boolean active, String search) {
        return squadRepository.findWithFilters(type, active, search);
    }

    public Optional<Squad> getSquadById(Long id) {
        return squadRepository.findById(id);
    }

    public Optional<Squad> getSquadByIdWithLead(Long id) {
        return squadRepository.findByIdWithLead(id);
    }

    public List<SquadMember> getMySquads(User user) {
        return squadMemberRepository.findByUser(user);
    }

    public List<Squad> getSquadsByMember(User user) {
        return squadMemberRepository.findByUser(user).stream()
                .map(SquadMember::getSquad)
                .filter(Squad::isActive)
                .toList();
    }

    public boolean isUserMemberOfSquad(User user, Squad squad) {
        return squadMemberRepository.existsBySquadAndUser(squad, user);
    }

    public Long getMemberCount(Long squadId) {
        return squadRepository.countMembersBySquadId(squadId);
    }

    // ========== Dashboard Metrics ==========

    // ========== Dashboard Metrics ==========

    @Transactional(readOnly = true)
    public SquadDashboardDTO getSquadDashboard(Long squadId, User currentUser) {
        Squad squad =
                squadRepository
                        .findById(squadId)
                        .orElseThrow(() -> new ResourceNotFoundException("Squad not found"));

        validateSquadAccess(squad, currentUser);

        LocalDateTime now = LocalDateTime.now();

        boolean isMember = currentUser.getRole() == Role.MEMBER;

        Long totalTasks;
        Long inProgressTasks;
        Long completedTasks;
        Long todoTasks;
        Long overdueTasks;
        List<Task> activeTasks;

        if (isMember) {
            List<Task> memberTasks =
                    taskRepository.findBySquadIdAndAssignedUserId(squadId, currentUser.getId());
            totalTasks = (long) memberTasks.size();
            inProgressTasks =
                    memberTasks.stream()
                            .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                            .count();
            completedTasks =
                    taskRepository.countCompletedBySquadIdAndUserId(squadId, currentUser.getId());
            todoTasks = memberTasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
            overdueTasks =
                    memberTasks.stream()
                            .filter(
                                    t ->
                                            t.getDueDate() != null
                                                    && t.getDueDate().isBefore(now)
                                                    && t.getStatus() != TaskStatus.DONE
                                                    && t.getStatus() != TaskStatus.CANCELLED)
                            .count();
            // MEMBER: only see their own active tasks
            activeTasks =
                    memberTasks.stream()
                            .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                            .toList();
        } else {
            // LEAD and ADMIN see all squad metrics
            totalTasks = taskRepository.countBySquadId(squadId);
            inProgressTasks =
                    taskRepository.countBySquadIdAndStatus(squadId, TaskStatus.IN_PROGRESS);
            completedTasks = taskRepository.countBySquadIdAndStatus(squadId, TaskStatus.DONE);
            todoTasks = taskRepository.countBySquadIdAndStatus(squadId, TaskStatus.TODO);
            overdueTasks = taskRepository.countOverdueBySquadId(squadId, now);
            activeTasks =
                    taskRepository.findActiveTimerTasksBySquadId(squadId, TaskStatus.IN_PROGRESS);
        }

        Long memberCount = squadRepository.countMembersBySquadId(squadId);
        List<SquadMember> members = squadMemberRepository.findBySquad(squad);

        List<MemberMetricsDTO> memberMetrics;

        if (isMember) {
            List<Task> memberTasks =
                    taskRepository.findBySquadIdAndAssignedUserId(squadId, currentUser.getId());
            Long completedByMember =
                    taskRepository.countCompletedBySquadIdAndUserId(squadId, currentUser.getId());
            long inProgressByMember =
                    memberTasks.stream()
                            .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                            .count();

            SquadMember currentMember =
                    members.stream()
                            .filter(m -> m.getUser().getId().equals(currentUser.getId()))
                            .findFirst()
                            .orElse(null);

            memberMetrics =
                    List.of(
                            new MemberMetricsDTO(
                                    currentUser.getId(),
                                    currentUser.getFullName(),
                                    currentUser.getUsername(),
                                    memberTasks.size(),
                                    completedByMember.intValue(),
                                    (int) inProgressByMember,
                                    currentMember != null ? currentMember.getJoinedAt() : null));
        } else {
            memberMetrics =
                    members.stream()
                            .map(
                                    member -> {
                                        User user = member.getUser();
                                        List<Task> memberTasks =
                                                taskRepository.findBySquadIdAndAssignedUserId(
                                                        squadId, user.getId());
                                        Long completedByMember =
                                                taskRepository.countCompletedBySquadIdAndUserId(
                                                        squadId, user.getId());
                                        long inProgressByMember =
                                                memberTasks.stream()
                                                        .filter(
                                                                t ->
                                                                        t.getStatus()
                                                                                == TaskStatus
                                                                                        .IN_PROGRESS)
                                                        .count();

                                        return new MemberMetricsDTO(
                                                user.getId(),
                                                user.getFullName(),
                                                user.getUsername(),
                                                memberTasks.size(),
                                                completedByMember.intValue(),
                                                (int) inProgressByMember,
                                                member.getJoinedAt());
                                    })
                            .toList();
        }

        return new SquadDashboardDTO(
                squad,
                totalTasks.intValue(),
                inProgressTasks.intValue(),
                completedTasks.intValue(),
                todoTasks.intValue(),
                overdueTasks.intValue(),
                memberCount.intValue(),
                activeTasks,
                memberMetrics);
    }

    // ========== Invite Operations ==========

    @Transactional
    public void inviteUser(Long squadId, String email, User invitedBy) {
        Squad squad =
                squadRepository
                        .findById(squadId)
                        .orElseThrow(() -> new ResourceNotFoundException("Squad not found"));

        validateSquadAccess(squad, invitedBy);

        if (!squad.isActive()) {
            throw new IllegalStateException("Cannot invite to an inactive squad");
        }

        User invitedUser =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User not found with email: " + email));

        if (squadMemberRepository.existsBySquadAndUser(squad, invitedUser)) {
            throw new IllegalArgumentException("User is already a member");
        }

        // Verificar limite de membros
        if (squad.getMaxMembers() != null) {
            Long currentMembers = squadRepository.countMembersBySquadId(squadId);
            if (currentMembers >= squad.getMaxMembers()) {
                throw new IllegalStateException("Squad has reached maximum member limit");
            }
        }

        SquadInvite invite =
                SquadInvite.builder()
                        .squad(squad)
                        .invitedUser(invitedUser)
                        .invitedBy(invitedBy)
                        .status(SquadInviteStatus.PENDING)
                        .build();
        squadInviteRepository.save(invite);
        webSocketService.notifySquadInvite(invite);

        notificationService.createStickyNotification(
                "Convite de Squad",
                "Você foi convidado para participar da squad '"
                        + squad.getName()
                        + "' por "
                        + invitedBy.getFullName(),
                NotificationType.SQUAD_INVITE,
                invite.getId(), // Using taskId field to store inviteId
                invitedUser);

        log.info(
                "Invite sent to {} for squad '{}' by {}",
                email,
                squad.getName(),
                invitedBy.getUsername());
    }

    public List<SquadInvite> getPendingInvites(User user) {
        return squadInviteRepository.findByInvitedUserAndStatus(user, SquadInviteStatus.PENDING);
    }

    public List<SquadInvite> getInviteHistory(User user) {
        return squadInviteRepository.findByInvitedUserAndStatusIn(
                user, List.of(SquadInviteStatus.ACCEPTED, SquadInviteStatus.REJECTED));
    }

    @Transactional
    public void respondToInvite(Long inviteId, boolean accept, User user) {
        SquadInvite invite =
                squadInviteRepository
                        .findByIdWithDetails(inviteId)
                        .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        if (!invite.getInvitedUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("This invite is not for you");
        }

        if (invite.getStatus() != SquadInviteStatus.PENDING) {
            throw new IllegalStateException("This invite has already been processed");
        }

        if (accept) {
            invite.setStatus(SquadInviteStatus.ACCEPTED);
            SquadMember member = SquadMember.builder().squad(invite.getSquad()).user(user).build();
            squadMemberRepository.save(member);
            webSocketService.notifySquadMemberJoined(invite.getSquad(), user);
            webSocketService.notifyRankingUpdate(invite.getSquad());
            log.info("User {} joined squad '{}'", user.getUsername(), invite.getSquad().getName());
        } else {
            invite.setStatus(SquadInviteStatus.REJECTED);
            log.info(
                    "User {} rejected invite to squad '{}'",
                    user.getUsername(),
                    invite.getSquad().getName());
        }
        invite.setRespondedAt(LocalDateTime.now());
        squadInviteRepository.save(invite);
    }

    // ========== Member Operations ==========

    public List<SquadMember> getSquadMembers(Squad squad) {
        return squadMemberRepository.findBySquadId(squad.getId());
    }

    public List<SquadInvite> getSquadInvites(Squad squad) {
        return squadInviteRepository.findBySquadAndStatus(squad, SquadInviteStatus.PENDING);
    }

    @Transactional
    public void removeMember(Long squadId, Long userId, User currentUser) {
        Squad squad =
                squadRepository
                        .findById(squadId)
                        .orElseThrow(() -> new ResourceNotFoundException("Squad not found"));

        validateSquadAccess(squad, currentUser);

        User userToRemove =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (squad.getLead().getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot remove the squad lead");
        }

        SquadMember member =
                squadMemberRepository
                        .findBySquadAndUser(squad, userToRemove)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User is not a member of this squad"));

        squadMemberRepository.delete(member);
        log.info(
                "User {} removed from squad '{}' by {}",
                userToRemove.getUsername(),
                squad.getName(),
                currentUser.getUsername());
    }

    // ========== Validation ==========

    private void validateSquadAccess(Squad squad, User user) {
        if (user.getRole() == Role.ADMIN) {
            log.debug(
                    "[SquadAccess] ADMIN {} tem acesso à squad {}",
                    user.getUsername(),
                    squad.getId());
            return;
        }

        if (squad.getLead().getId().equals(user.getId())) {
            log.debug("[SquadAccess] LEAD {} lidera a squad {}", user.getUsername(), squad.getId());
            return;
        }

        boolean isMember = squadMemberRepository.existsBySquadAndUser(squad, user);
        log.debug(
                "[SquadAccess] MEMBER check userId={} squadId={} exists={} ",
                user.getId(),
                squad.getId(),
                isMember);

        if (!isMember) {
            throw new UnauthorizedException("Você não é membro desta squad");
        }
    }

    private <T> void updateIfNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
