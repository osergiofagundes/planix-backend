package com.sergio.planix.team.dto;

import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.team.Team;
import com.sergio.planix.team.TeamRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Uma equipe. Os quadros dela não vêm aqui — busque-os em "
                    + "`/api/boards?teamId={id}`.")
public record TeamResponse(
        @Schema(description = "Id da equipe", example = "1") Long id,
        @Schema(description = "Nome da equipe", example = "Acme") String name,
        @Schema(description = "Descrição livre", example = "Todo mundo da Acme, de todos os times.")
        String description,
        @Schema(description = "Chave do ícone da equipe", example = "building-2") String icon,
        @Schema(description = "Dono da equipe") UserSummary owner,
        @Schema(description = "O **seu** papel nesta equipe") TeamRole myRole,
        @Schema(description = "Quantas pessoas participam da equipe", example = "8")
        long memberCount,
        @Schema(description = "Quantos quadros desta equipe **você** enxerga. Um membro comum não "
                            + "conta os quadros fechados de que não participa.", example = "5")
        long boardCount,
        @Schema(description = "Quando a equipe foi criada", example = "2026-07-15T09:12:44.518-03:00")
        OffsetDateTime createdAt,
        @Schema(description = "Última alteração na equipe", example = "2026-08-01T14:32:10.123-03:00")
        OffsetDateTime updatedAt
) {
    public static TeamResponse of(Team team, TeamRole myRole, long memberCount, long boardCount) {
        return new TeamResponse(team.getId(), team.getName(), team.getDescription(), team.getIcon(),
                UserSummary.from(team.getOwner()), myRole, memberCount, boardCount,
                team.getCreatedAt(), team.getUpdatedAt());
    }
}
