package com.workflow.demo.service;

import com.workflow.demo.entity.Team;
import com.workflow.demo.entity.TeamMember;
import com.workflow.demo.entity.TeamMember.Status;
import com.workflow.demo.entity.User;
import com.workflow.demo.repository.TeamRepository;
import com.workflow.demo.repository.TeamMemberRepository;
import com.workflow.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;

@Service
public class TeamService {
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public TeamService(TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Team createTeam(String name, UUID ownerId, String ownerEmail) {

        Team team = Team.builder()
                .name(name)
                .ownerId(ownerId)
                .createdAt(OffsetDateTime.now())
                .build();

        Team saved = teamRepository.save(team);

        TeamMember ownerMember = TeamMember.builder()
                .team(saved)
                .userId(ownerId)
                .email(ownerEmail)
                .status(Status.ACCEPTED)
                .invitedAt(OffsetDateTime.now())
                .acceptedAt(OffsetDateTime.now())
                .build();

        teamMemberRepository.save(ownerMember);

        return saved;
    }


    @Transactional
    public TeamMember inviteMember(UUID teamId, String email) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("team not found"));

        // don't create duplicate invite if exists
        Optional<TeamMember> existing = teamMemberRepository.findByTeamIdAndEmail(teamId, email);
        if (existing.isPresent()) {
            TeamMember em = existing.get();
            // if previously removed, re-invite
            em.setStatus(Status.INVITED);
            em.setInvitedAt(OffsetDateTime.now());
            em.setAcceptedAt(null);
            return teamMemberRepository.save(em);
        }

        // if user exists, attach userId (optional)
        Optional<User> maybe = userRepository.findByEmail(email);
        UUID userId = maybe.map(User::getId).orElse(null);

        TeamMember member = TeamMember.builder()
                .team(team)
                .email(email)
                .userId(userId)
                .status(Status.INVITED)
                .invitedAt(OffsetDateTime.now())
                .build();
        return teamMemberRepository.save(member);
    }

    @Transactional
    public TeamMember acceptInvite(UUID teamId, UUID inviteId, UUID acceptingUserId) {
        TeamMember invite = teamMemberRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("invite not found"));

        if (!invite.getTeam().getId().equals(teamId)) {
            throw new IllegalArgumentException("invite does not belong to team");
        }

        // verify accepting user matches invite email (if userId known) or allow if emails match
        Optional<User> userOpt = userRepository.findById(acceptingUserId);
        if (userOpt.isEmpty()) throw new IllegalArgumentException("user not found");

        User user = userOpt.get();
        if (!user.getEmail().equalsIgnoreCase(invite.getEmail())) {
            throw new IllegalArgumentException("invite email does not match accepting user");
        }

        invite.setUserId(acceptingUserId);
        invite.setStatus(Status.ACCEPTED);
        invite.setAcceptedAt(OffsetDateTime.now());
        return teamMemberRepository.save(invite);
    }

    public List<Team> listTeamsForOwner(UUID ownerId) {
        return teamRepository.findByOwnerId(ownerId);
    }

    public List<TeamMember> listMembers(UUID teamId) {
        return teamMemberRepository.findByTeamId(teamId);
    }


}
