package com.sergio.planix.comment;

import com.sergio.planix.card.Card;
import com.sergio.planix.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "comments")
@Getter
@Setter
public class Comment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    protected Comment() {}

    public Comment(Card card, String text) {
        this.card = card;
        this.text = text;
    }
}
