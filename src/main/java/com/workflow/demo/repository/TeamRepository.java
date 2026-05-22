package com.workflow.demo.repository;

import com.workflow.demo.entity.Team;
import com.workflow.demo.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByOwnerId(UUID ownerId);

    @Query("""
            select distinct t
            from Team t
            left join t.members m
            where t.ownerId = :userId
               or (m.userId = :userId and m.status = :status)
            """)
    List<Team> findVisibleTeamsForUser(
            @Param("userId") UUID userId,
            @Param("status") TeamMember.Status status
    );
}
