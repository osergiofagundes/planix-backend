package com.sergio.planix.team.dto;

import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.team.TeamMember;
import com.sergio.planix.team.TeamRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Alguém dentro da equipe, com o papel que exerce nela.")
public record TeamMemberResponse(
        @Schema(description = "O usuário") UserSummary user,
        @Schema(description = "O papel dele na equipe") TeamRole role,
        @Schema(description = "Quando entrou na equipe", example = "2026-07-15T09:12:44.518-03:00")
        OffsetDateTime joinedAt
) {
    public static TeamMemberResponse from(TeamMember member) {
        return new TeamMemberResponse(UserSummary.from(member.getUser()), member.getRole(),
                member.getCreatedAt());
    }
}
