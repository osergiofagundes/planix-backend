package com.sergio.planix.board.dto;

import com.sergio.planix.board.Board;
import java.time.OffsetDateTime;

public record BoardResponse(
        Long id, String name, String description,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static BoardResponse from(Board board) {
        return new BoardResponse(board.getId(), board.getName(), board.getDescription(), board.getCreatedAt(), board.getUpdatedAt());
    }
}