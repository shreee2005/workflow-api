package com.workflow.demo.repository;

import com.workflow.demo.entity.TeamMember;
import com.workflow.demo.entity.TeamMember.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {
    @Query("select m from TeamMember m where m.team.id = :teamId")
    List<TeamMember> findByTeamId(@Param("teamId") UUID teamId);

    @Query("select m from TeamMember m where m.team.id = :teamId and m.status = :status")
    List<TeamMember> findByTeamIdAndStatus(@Param("teamId") UUID teamId, @Param("status") Status status);

    @Query("select m from TeamMember m where m.team.id = :teamId and lower(m.email) = lower(:email)")
    Optional<TeamMember> findByTeamIdAndEmail(@Param("teamId") UUID teamId, @Param("email") String email);

    @Query("select m from TeamMember m join fetch m.team where lower(m.email) = lower(:email) and m.status = :status")
    List<TeamMember> findByEmailAndStatus(@Param("email") String email, @Param("status") Status status);
}
