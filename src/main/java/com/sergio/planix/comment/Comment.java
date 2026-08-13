package com.sergio.planix.comment;

import com.sergio.planix.auth.User;
import com.sergio.planix.card.Card;
import com.sergio.planix.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
public class Comment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Comment() {}

    public Comment(Card card, User author, String text, Comment parent) {
        this.card = card;
        this.author = author;
        this.text = text;
        this.parent = parent;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
