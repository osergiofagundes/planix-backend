package com.sergio.planix.team.dto;

import com.sergio.planix.team.TeamRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "O novo papel de alguém na equipe.")
public record RoleChangeRequest(
        @Schema(description = "`ADMIN` ou `MEMBER`. Para tornar alguém `OWNER` use a "
                            + "transferência de posse.", example = "ADMIN")
        @NotNull TeamRole role
) {}
