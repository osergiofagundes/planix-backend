package com.sergio.planix.invite;

import com.sergio.planix.auth.User;
import com.sergio.planix.team.Team;
import com.sergio.planix.team.TeamRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "team_invites")
@Getter
@Setter
public class TeamInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamRole role;

    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    @Column(nullable = false)
    private int uses;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected TeamInvite() {}

    public TeamInvite(Team team, User createdBy, String tokenHash, TeamRole role, int maxUses,
                      OffsetDateTime expiresAt) {
        this.team = team;
        this.createdBy = createdBy;
        this.tokenHash = tokenHash;
        this.role = role;
        this.maxUses = maxUses;
        this.uses = 0;
        this.expiresAt = expiresAt;
        this.createdAt = OffsetDateTime.now();
    }

    public boolean isExpired()   { return expiresAt.isBefore(OffsetDateTime.now()); }

    public boolean isRevoked()   { return revokedAt != null; }

    public boolean isExhausted() { return uses >= maxUses; }

    public boolean isUsable()    { return !isExpired() && !isRevoked() && !isExhausted(); }
}
