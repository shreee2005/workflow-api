package com.workflow.demo.repository;

import com.workflow.demo.entity.TeamMember;
import com.workflow.demo.entity.TeamMember.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {
    List<TeamMember> findByTeamId(UUID teamId);
    Optional<TeamMember> findByTeamIdAndEmail(UUID teamId, String email);
    List<TeamMember> findByEmailAndStatus(String email, Status status);
}
