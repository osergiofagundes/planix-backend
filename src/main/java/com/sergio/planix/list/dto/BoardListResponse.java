package com.sergio.planix.list.dto;

import com.sergio.planix.list.BoardList;

import java.time.OffsetDateTime;

public record BoardListResponse(
        Long id, Long boardId, String name, int position,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static BoardListResponse from(BoardList list) {
        return new BoardListResponse(list.getId(), list.getBoard().getId(), list.getName(),
                list.getPosition(), list.getCreatedAt(), list.getUpdatedAt());
    }
}
