package com.sergio.planix.checklist;

import com.sergio.planix.card.Card;
import com.sergio.planix.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "checklist_items")
@Getter
@Setter
public class ChecklistItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private boolean done = false;

    @Column(nullable = false)
    private int position;

    protected ChecklistItem() {}

    public ChecklistItem(Card card, String text, int position) {
        this.card = card;
        this.text = text;
        this.position = position;
    }
}
