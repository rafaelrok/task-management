package br.com.rafaelvieira.taskmanagement.service;

import br.com.rafaelvieira.taskmanagement.domain.model.Squad;
import br.com.rafaelvieira.taskmanagement.domain.model.SquadInvite;
import br.com.rafaelvieira.taskmanagement.domain.model.TaskCancelRequest;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.domain.model.UserBadge;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/** Service for sending real-time notifications via WebSocket for gamification and squad events */
@Service
@RequiredArgsConstructor
public class GamificationWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /** Notify user about new badge earned */
    public void notifyBadgeEarned(User user, UserBadge userBadge) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "BADGE_EARNED");
        payload.put("badgeCode", userBadge.getBadge().getCode());
        payload.put("badgeName", userBadge.getBadge().getName());
        payload.put("badgeDescription", userBadge.getBadge().getDescription());
        payload.put("badgeIcon", userBadge.getBadge().getIconClass());
        payload.put("awardedAt", userBadge.getAwardedAt());

        sendToUser(user.getUsername(), "/queue/badges", payload);
    }

    /** Notify user about points earned */
    public void notifyPointsEarned(User user, int points, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "POINTS_EARNED");
        payload.put("points", points);
        payload.put("reason", reason);
        payload.put("timestamp", java.time.LocalDateTime.now());

        sendToUser(user.getUsername(), "/queue/points", payload);
    }

    /** Notify user about squad invite */
    public void notifySquadInvite(SquadInvite invite) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "SQUAD_INVITE");
        payload.put("inviteId", invite.getId());
        payload.put("squadName", invite.getSquad().getName());
        payload.put("squadDescription", invite.getSquad().getDescription());
        payload.put("invitedBy", invite.getInvitedBy().getFullName());
        payload.put("createdAt", invite.getCreatedAt());

        sendToUser(invite.getInvitedUser().getUsername(), "/queue/squad-invites", payload);
    }

    /** Notify squad lead about new cancellation request */
    public void notifyCancellationRequest(TaskCancelRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "CANCEL_REQUEST");
        payload.put("requestId", request.getId());
        payload.put("taskTitle", request.getTask().getTitle());
        payload.put("requestedBy", request.getRequestedBy().getFullName());
        payload.put("reason", request.getReason());
        payload.put("createdAt", request.getCreatedAt());

        sendToUser(request.getLeadToApprove().getUsername(), "/queue/cancel-requests", payload);
    }

    /** Notify user about cancellation request decision */
    public void notifyCancellationDecision(TaskCancelRequest request, boolean approved) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "CANCEL_DECISION");
        payload.put("requestId", request.getId());
        payload.put("taskTitle", request.getTask().getTitle());
        payload.put("approved", approved);
        payload.put("decidedAt", request.getDecidedAt());

        sendToUser(request.getRequestedBy().getUsername(), "/queue/cancel-decisions", payload);
    }

    /** Notify all squad members about a new member joining */
    public void notifySquadMemberJoined(Squad squad, User newMember) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "SQUAD_MEMBER_JOINED");
        payload.put("squadId", squad.getId());
        payload.put("squadName", squad.getName());
        payload.put("memberName", newMember.getFullName());
        payload.put("timestamp", java.time.LocalDateTime.now());

        // Send to squad topic (all members subscribed)
        sendToTopic("/topic/squad/" + squad.getId(), payload);
    }

    /** Notify squad about ranking update */
    public void notifyRankingUpdate(Squad squad) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "RANKING_UPDATE");
        payload.put("squadId", squad.getId());
        payload.put("timestamp", java.time.LocalDateTime.now());

        sendToTopic("/topic/squad/" + squad.getId() + "/ranking", payload);
    }

    /** Send message to specific user */
    private void sendToUser(String username, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(username, destination, payload);
        } catch (ResourceNotFoundException e) {
            System.err.println(
                    "Error sending WebSocket message to user " + username + ": " + e.getMessage());
        }
    }

    /** Send message to topic (broadcast) */
    private void sendToTopic(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (ResourceNotFoundException e) {
            System.err.println(
                    "Error sending WebSocket message to topic "
                            + destination
                            + ": "
                            + e.getMessage());
        }
    }
}
