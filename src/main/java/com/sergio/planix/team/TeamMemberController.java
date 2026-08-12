package com.sergio.planix.team;

import com.sergio.planix.team.dto.RoleChangeRequest;
import com.sergio.planix.team.dto.TeamMemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams/{teamId}/members")
@Tag(name = "Membros da equipe")
public class TeamMemberController {

    private final TeamMemberService service;

    public TeamMemberController(TeamMemberService service) { this.service = service; }

    @Operation(summary = "Listar quem está na equipe",
               description = "Qualquer membro pode ver a lista, com o papel de cada um. Vem na "
                           + "ordem de entrada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membros da equipe"),
            @ApiResponse(responseCode = "404", description = "Equipe não encontrada")
    })
    @GetMapping
    public List<TeamMemberResponse> list(@Parameter(description = "Id da equipe") @PathVariable Long teamId) {
        return service.list(teamId);
    }

    @Operation(summary = "Mudar o papel de alguém na equipe",
               description = "Só o dono pode, e só entre `ADMIN` e `MEMBER` — tornar alguém dono é "
                           + "transferência de posse, em `PATCH /api/teams/{id}/owner`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Papel atualizado"),
            @ApiResponse(responseCode = "403", description = "Você não é o dono, ou tentou mexer no papel do dono"),
            @ApiResponse(responseCode = "404", description = "Equipe não encontrada, ou esse usuário não é membro dela")
    })
    @PatchMapping("/{userId}")
    public TeamMemberResponse changeRole(@Parameter(description = "Id da equipe") @PathVariable Long teamId,
                                         @Parameter(description = "Id do usuário") @PathVariable Long userId,
                                         @Valid @RequestBody RoleChangeRequest req) {
        return service.changeRole(teamId, userId, req.role());
    }

    @Operation(summary = "Sair da equipe",
               description = "Remove **você** da equipe, dos quadros dela e dos cartões em que era "
                           + "responsável. O dono não pode sair da própria equipe — para ele a "
                           + "saída é transferir a posse ou excluir a equipe.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Você saiu da equipe", content = @Content),
            @ApiResponse(responseCode = "403", description = "Você é o dono e não pode sair da própria equipe"),
            @ApiResponse(responseCode = "404", description = "Equipe não encontrada")
    })
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@Parameter(description = "Id da equipe") @PathVariable Long teamId) {
        service.leave(teamId);
    }

    @Operation(summary = "Remover alguém da equipe",
               description = "Dono e admins podem, mas só o dono remove outro admin, e ninguém "
                           + "remove o dono. A pessoa perde o acesso a todos os quadros da equipe "
                           + "e as atribuições que tinha nos cartões deles.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membro removido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Papel insuficiente, ou tentativa de remover o dono"),
            @ApiResponse(responseCode = "404", description = "Equipe não encontrada, ou esse usuário não é membro dela")
    })
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@Parameter(description = "Id da equipe") @PathVariable Long teamId,
                       @Parameter(description = "Id do usuário a remover") @PathVariable Long userId) {
        service.remove(teamId, userId);
    }
}
