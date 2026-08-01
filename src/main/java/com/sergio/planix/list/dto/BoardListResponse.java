package com.sergio.planix.list.dto;

import com.sergio.planix.list.BoardList;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Uma lista (coluna) do quadro. Os cartões vêm em `/api/lists/{listId}/cards`.")
public record BoardListResponse(
        @Schema(description = "Id da lista", example = "10") Long id,
        @Schema(description = "Quadro a que a lista pertence", example = "1") Long boardId,
        @Schema(description = "Nome da lista", example = "A Fazer") String name,
        @Schema(description = "Ordem da lista dentro do quadro, começando em 0", example = "0") int position,
        @Schema(description = "Quando a lista foi criada", example = "2026-07-15T09:12:44.518-03:00")
        OffsetDateTime createdAt,
        @Schema(description = "Última alteração na lista", example = "2026-08-01T14:32:10.123-03:00")
        OffsetDateTime updatedAt
) {
    public static BoardListResponse from(BoardList list) {
        return new BoardListResponse(list.getId(), list.getBoard().getId(), list.getName(),
                list.getPosition(), list.getCreatedAt(), list.getUpdatedAt());
    }
}
