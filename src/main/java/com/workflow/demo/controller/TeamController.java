package com.workflow.demo.controller;

import com.workflow.demo.entity.Team;
import com.workflow.demo.entity.TeamMember;
import com.workflow.demo.service.TeamService;
import com.workflow.demo.repository.UserRepository;
import com.workflow.demo.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/teams")
public class TeamController {
    private final TeamService teamService;
    private final UserRepository userRepository;

    public TeamController(TeamService teamService, UserRepository userRepository) {
        this.teamService = teamService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> createTeam(@RequestBody Map<String, String> body) {

        String name = body.get("name");
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name_required");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID ownerId = currentUserId();

        String ownerEmail = (String) auth.getCredentials();
        if (ownerEmail == null || ownerEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "email_missing_in_token");
        }

        Team team = teamService.createTeam(name.trim(), ownerId, ownerEmail);

        return ResponseEntity
                .created(URI.create("/api/teams/" + team.getId()))
                .body(Map.of("teamId", team.getId()));
    }


    @GetMapping
    public ResponseEntity<List<TeamView>> listMyTeams() {
        UUID userId = currentUserId();
        List<TeamView> teams = teamService.listTeamsForOwner(userId)
                .stream()
                .map(t -> new TeamView(
                        t.getId(),
                        t.getName(),
                        t.getOwnerId(),
                        t.getOwnerId().equals(userId) ? "OWNER" : "MEMBER",
                        t.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<MemberView>> listMembers(@PathVariable UUID teamId) {
        List<MemberView> members = teamService.listMembers(teamId, currentUserId())
                .stream()
                .map(m -> new MemberView(
                        m.getId(),
                        m.getEmail(),
                        m.getUserId(),
                        m.getStatus() != null ? m.getStatus().name() : null,
                        m.getInvitedAt(),
                        m.getAcceptedAt()
                ))
                .toList();
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{teamId}/invite")
    public ResponseEntity<?> invite(@PathVariable UUID teamId, @RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email_required");
        }
        TeamMember m = teamService.inviteMember(teamId, email.trim().toLowerCase());
        return ResponseEntity.ok(Map.of(
                "inviteId", m.getId(),
                "email", m.getEmail(),
                "teamId", teamId,
                "teamName", m.getTeam().getName(),
                "status", m.getStatus().name()
        ));
    }

    @PostMapping("/{teamId}/invites/{inviteId}/accept")
    public ResponseEntity<?> acceptInvite(@PathVariable UUID teamId, @PathVariable UUID inviteId) {
        UUID userId = currentUserId();
        TeamMember m = teamService.acceptInvite(teamId, inviteId, userId);
        return ResponseEntity.ok(Map.of("status", m.getStatus()));
    }

    @GetMapping("/invitations")
    public ResponseEntity<List<InvitationView>> listMyInvitations() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (String) auth.getCredentials();
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email_not_found_in_session");
        }

        List<InvitationView> invitations = teamService.listInvitationsForEmail(email)
                .stream()
                .map(m -> new InvitationView(
                        m.getId(),
                        m.getTeam().getId(),
                        m.getTeam().getName(),
                        m.getInvitedAt()
                ))
                .toList();
        return ResponseEntity.ok(invitations);
    }

    @PostMapping("/{teamId}/invites/{inviteId}/decline")
    public ResponseEntity<?> declineInvite(@PathVariable UUID teamId, @PathVariable UUID inviteId) {
        UUID userId = currentUserId();
        TeamMember m = teamService.declineInvite(teamId, inviteId, userId);
        return ResponseEntity.ok(Map.of("status", m.getStatus()));
    }

    @GetMapping("/{teamId}/invites/pending")
    public ResponseEntity<List<MemberView>> listPendingInvitations(@PathVariable UUID teamId) {
        List<MemberView> members = teamService.listPendingInvitations(teamId, currentUserId())
                .stream()
                .map(m -> new MemberView(
                        m.getId(),
                        m.getEmail(),
                        m.getUserId(),
                        m.getStatus() != null ? m.getStatus().name() : null,
                        m.getInvitedAt(),
                        m.getAcceptedAt()
                ))
                .toList();
        return ResponseEntity.ok(members);
    }

    @DeleteMapping("/{teamId}/invites/{inviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelInvitation(@PathVariable UUID teamId, @PathVariable UUID inviteId) {
        teamService.cancelInvitation(teamId, inviteId, currentUserId());
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthenticated");
        }

        if (auth.getPrincipal() instanceof UUID userId) {
            return userId;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unexpected_principal_type");
    }

    public record MemberView(
            UUID id,
            String email,
            UUID userId,
            String status,
            OffsetDateTime invitedAt,
            OffsetDateTime acceptedAt
    ) {}

    public record InvitationView(
            UUID inviteId,
            UUID teamId,
            String teamName,
            OffsetDateTime invitedAt
    ) {}

    public record TeamView(
            UUID id,
            String name,
            UUID ownerId,
            String role,
            OffsetDateTime createdAt
    ) {}
}
