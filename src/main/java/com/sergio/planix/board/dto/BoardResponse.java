package com.sergio.planix.board.dto;

import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.board.Board;
import com.sergio.planix.board.BoardVisibility;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Um quadro. As listas não vêm aqui — busque-as em `/api/boards/{boardId}/lists`.")
public record BoardResponse(
        @Schema(description = "Id do quadro", example = "1") Long id,
        @Schema(description = "Id da equipe a que o quadro pertence", example = "1") Long teamId,
        @Schema(description = "Nome do quadro", example = "Lançamento do site") String name,
        @Schema(description = "Descrição livre", example = "Tudo que precisa sair antes de colocar o site no ar.")
        String description,
        @Schema(description = "Chave do ícone do quadro", example = "rocket")
        String icon,
        @Schema(description = "Quem da equipe enxerga o quadro") BoardVisibility visibility,
        @Schema(description = "Dono do quadro — ele e quem administra a equipe podem configurá-lo")
        UserSummary owner,
        @Schema(description = "Quando o quadro foi criado", example = "2026-07-15T09:12:44.518-03:00")
        OffsetDateTime createdAt,
        @Schema(description = "Última alteração no quadro", example = "2026-08-01T14:32:10.123-03:00")
        OffsetDateTime updatedAt
) {
    public static BoardResponse from(Board board) {
        return new BoardResponse(board.getId(), board.getTeam().getId(), board.getName(),
                board.getDescription(), board.getIcon(), board.getVisibility(),
                UserSummary.from(board.getOwner()), board.getCreatedAt(), board.getUpdatedAt());
    }
}
