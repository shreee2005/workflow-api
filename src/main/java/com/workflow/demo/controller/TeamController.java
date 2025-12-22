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

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/teams")
public class TeamController {
    private final TeamService teamService;
    private final UserRepository userRepository;

    public TeamController(TeamService teamService, UserRepository userRepository) {
        this.teamService = teamService;
        this.userRepository = userRepository;
    }

    /** Create a team. Body: { "name": "Acme Team" } -> returns 201 with created resource and id */
    @PostMapping
    public ResponseEntity<?> createTeam(@RequestBody Map<String, String> body) {

        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name_required"));
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID ownerId = currentUserId();

        String ownerEmail = (String) auth.getCredentials();
        if (ownerEmail == null || ownerEmail.isBlank()) {
            return ResponseEntity.status(500).body(Map.of("error", "email_missing_in_token"));
        }

        Team team = teamService.createTeam(name.trim(), ownerId, ownerEmail);

        return ResponseEntity
                .created(URI.create("/api/teams/" + team.getId()))
                .body(Map.of("teamId", team.getId()));
    }


    @GetMapping
    public ResponseEntity<List<Team>> listMyTeams() {
        UUID ownerId = currentUserId();
        return ResponseEntity.ok(teamService.listTeamsForOwner(ownerId));
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMember>> listMembers(@PathVariable UUID teamId) {
        return ResponseEntity.ok(teamService.listMembers(teamId));
    }

    /** Invite a user by email */
    @PostMapping("/{teamId}/invite")
    public ResponseEntity<?> invite(@PathVariable UUID teamId, @RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "email_required"));
        TeamMember m = teamService.inviteMember(teamId, email.trim().toLowerCase());
        return ResponseEntity.ok(Map.of("inviteId", m.getId()));
    }

    /** Accept an invite (user must be authenticated and email must match invite) */
    @PostMapping("/{teamId}/invites/{inviteId}/accept")
    public ResponseEntity<?> acceptInvite(@PathVariable UUID teamId, @PathVariable UUID inviteId) {
        UUID userId = currentUserId();
        TeamMember m = teamService.acceptInvite(teamId, inviteId, userId);
        return ResponseEntity.ok(Map.of("status", m.getStatus()));
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("unauthenticated request");
        }

        if (auth.getPrincipal() instanceof UUID userId) {
            return userId;
        }

        throw new IllegalStateException(
                "unexpected principal type: " + auth.getPrincipal().getClass().getName()
        );
    }

}
