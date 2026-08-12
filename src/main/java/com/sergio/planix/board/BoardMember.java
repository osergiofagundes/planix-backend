package com.sergio.planix.board;

import com.sergio.planix.auth.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "board_members")
@Getter
public class BoardMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id")
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected BoardMember() {}

    public BoardMember(Board board, User user) {
        this.board = board;
        this.user = user;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
