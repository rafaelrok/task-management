package br.com.rafaelvieira.taskmanagement.controller;

import br.com.rafaelvieira.taskmanagement.domain.model.SquadInvite;
import br.com.rafaelvieira.taskmanagement.domain.model.SquadMember;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.service.SquadService;
import br.com.rafaelvieira.taskmanagement.service.UserService;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/squads")
@RequiredArgsConstructor
public class SquadRestController {

    private final SquadService squadService;
    private final UserService userService;

    @GetMapping("/{squadId}/members")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getSquadMembers(
            @PathVariable("squadId") Long squadId) {
        try {
            System.out.println("Fetching members for squad ID: " + squadId);
            return squadService
                    .getSquadByIdWithLead(squadId)
                    .map(
                            squad -> {
                                System.out.println("Squad found: " + squad.getName());
                                List<SquadMember> members = squadService.getSquadMembers(squad);
                                // Log member count
                                System.out.println(
                                        "Squad "
                                                + squadId
                                                + " has "
                                                + members.size()
                                                + " members in DB");

                                List<Map<String, Object>> memberList =
                                        new java.util.ArrayList<>(
                                                members.stream()
                                                        .map(
                                                                member -> {
                                                                    User user = member.getUser();
                                                                    String displayName =
                                                                            user.getFullName()
                                                                                            != null
                                                                                    ? user
                                                                                            .getFullName()
                                                                                    : user
                                                                                            .getUsername();
                                                                    return Map.<String, Object>of(
                                                                            "id",
                                                                            user.getId(),
                                                                            "name",
                                                                            displayName,
                                                                            "username",
                                                                            user.getUsername(),
                                                                            "role",
                                                                            "MEMBER");
                                                                })
                                                        .toList());

                                // Also add the lead if not already in the list
                                User lead = squad.getLead();
                                boolean leadInList =
                                        memberList.stream()
                                                .anyMatch(m -> m.get("id").equals(lead.getId()));
                                if (!leadInList) {
                                    String leadDisplayName =
                                            lead.getFullName() != null
                                                    ? lead.getFullName()
                                                    : lead.getUsername();
                                    memberList.addFirst(
                                            Map.of(
                                                    "id",
                                                    lead.getId(),
                                                    "name",
                                                    leadDisplayName,
                                                    "username",
                                                    lead.getUsername(),
                                                    "role",
                                                    "LEAD"));
                                }

                                System.out.println(
                                        "Returning "
                                                + memberList.size()
                                                + " members (including lead) for squad "
                                                + squadId);

                                return ResponseEntity.ok(memberList);
                            })
                    .orElseGet(
                            () -> {
                                System.out.println("Squad not found for ID: " + squadId);
                                return ResponseEntity.ok(Collections.emptyList());
                            });
        } catch (ResourceNotFoundException e) {
            System.err.println("Error fetching squad members: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping("/invites/{id}/accept")
    public ResponseEntity<Void> acceptInvite(@PathVariable("id") Long id, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        squadService.respondToInvite(id, true, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invites/{id}/reject")
    public ResponseEntity<Void> rejectInvite(@PathVariable("id") Long id, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        squadService.respondToInvite(id, false, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-invites")
    public ResponseEntity<List<SquadInvite>> getMyInvites(Principal principal) {
        User user = userService.findByUsername(principal.getName());
        List<SquadInvite> invites = squadService.getPendingInvites(user);
        return ResponseEntity.ok(invites);
    }

    /** TODO: Remove a member from squad (LEAD or ADMIN only) */
    @DeleteMapping("/{squadId}/members/{userId}")
    @PreAuthorize("hasAnyRole('LEAD', 'ADMIN')")
    public ResponseEntity<Void> removeMember(
            @PathVariable("squadId") Long squadId,
            @PathVariable("userId") Long userId,
            Principal principal) {
        User currentUser = userService.findByUsername(principal.getName());

        // TODO: Implement member removal logic in SquadService
        // squadService.removeMember(squadId, userId, currentUser);

        return ResponseEntity.ok().build();
    }
}
