package com.sergio.planix.card;

import com.sergio.planix.auth.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "card_changes")
@Getter
public class CardChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User author;

    @Column(nullable = false)
    private String field;

    @Column(name = "old_value", columnDefinition = "text")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "text")
    private String newValue;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    protected CardChange() {}

    public CardChange(Card card, User author, String field, String oldValue, String newValue) {
        this.card = card;
        this.author = author;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
