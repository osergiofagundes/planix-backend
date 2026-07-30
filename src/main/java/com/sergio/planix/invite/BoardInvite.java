package com.sergio.planix.invite;

import com.sergio.planix.auth.User;
import com.sergio.planix.board.Board;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "board_invites")
@Getter
@Setter
public class BoardInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id")
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

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

    protected BoardInvite() {}

    public BoardInvite(Board board, User createdBy, String tokenHash, int maxUses,
                       OffsetDateTime expiresAt) {
        this.board = board;
        this.createdBy = createdBy;
        this.tokenHash = tokenHash;
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
