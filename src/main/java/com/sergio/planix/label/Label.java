package com.sergio.planix.label;

import com.sergio.planix.board.Board;
import com.sergio.planix.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "labels")
@Getter
@Setter
public class Label extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id")
    private Board board;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String color;

    protected Label() {}

    public Label(Board board, String name, String color) {
        this.board = board;
        this.name = name;
        this.color = color;
    }
}
