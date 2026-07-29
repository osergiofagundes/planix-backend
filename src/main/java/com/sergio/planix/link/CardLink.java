package com.sergio.planix.link;

import com.sergio.planix.card.Card;
import com.sergio.planix.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "card_links")
@Getter
@Setter
public class CardLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(nullable = false)
    private String url;

    private String title;

    protected CardLink() {}

    public CardLink(Card card, String url, String title) {
        this.card = card;
        this.url = url;
        this.title = title;
    }
}
