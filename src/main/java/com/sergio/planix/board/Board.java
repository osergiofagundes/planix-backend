package com.sergio.planix.board;

import com.sergio.planix.auth.User;
import com.sergio.planix.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "boards")
@Getter
@Setter
public class Board extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    protected Board() {}

    public Board(User owner, String name, String description) {
        this.owner = owner;
        this.name = name;
        this.description = description;
    }
}
