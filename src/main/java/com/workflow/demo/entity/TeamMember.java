package com.workflow.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "team_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"team_id", "email"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMember {
    public enum Status { INVITED, ACCEPTED, REMOVED }

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    // email of the invited user (may or may not have a user record yet)
    @Column(nullable = false)
    private String email;

    // if the invited user exists in users table, store their id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.INVITED;

    @Column(name = "invited_at", columnDefinition = "timestamp with time zone")
    private OffsetDateTime invitedAt = OffsetDateTime.now();

    @Column(name = "accepted_at", columnDefinition = "timestamp with time zone")
    private OffsetDateTime acceptedAt;
}
