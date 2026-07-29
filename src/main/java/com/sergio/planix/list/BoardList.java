package com.sergio.planix.list;

import com.sergio.planix.board.Board;
import com.sergio.planix.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "board_lists")
@Getter
@Setter
public class BoardList extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id")
    private Board board;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int position;

    protected BoardList() {}

    public BoardList(Board board, String name, int position) {
        this.board = board;
        this.name = name;
        this.position = position;
    }
}