package com.sergio.planix.comment;

import com.sergio.planix.auth.User;
import com.sergio.planix.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "comment_reactions")
@Getter
@Setter
public class CommentReaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String emoji;

    protected CommentReaction() {}

    public CommentReaction(Comment comment, User user, String emoji) {
        this.comment = comment;
        this.user = user;
        this.emoji = emoji;
    }
}
