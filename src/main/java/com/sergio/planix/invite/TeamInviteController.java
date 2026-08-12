package com.sergio.planix.invite;

import com.sergio.planix.invite.dto.*;
import com.sergio.planix.team.dto.TeamResponse;
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
@Tag(name = "Convites")
public class TeamInviteController {

    private final TeamInviteService service;

    public TeamInviteController(TeamInviteService service) { this.service = service; }

    @Operation(summary = "Gerar um link de convite para a equipe",
               description = """
                       Quem administra a equipe pode gerar. O token em texto claro aparece **apenas
                       nesta resposta** — o banco guarda só o hash, então não há como recuperá-lo
                       depois. Se perder, revogue e gere outro.

                       Quem aceitar entra na equipe, e daí passa a ver os quadros abertos a ela. Para
                       dar acesso a um quadro fechado, use `POST /api/boards/{boardId}/members`.

                       Padrões quando os campos são omitidos: 1 uso, 7 dias de validade e papel
                       `MEMBER`.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Convite criado; o token vem em `token`"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Você não administra esta equipe"),
            @ApiResponse(responseCode = "404", description = "Equipe não encontrada")
    })
    @PostMapping("/api/teams/{teamId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteCreatedResponse create(@Parameter(description = "Id da equipe") @PathVariable Long teamId,
                                        @Valid @RequestBody InviteRequest req) {
        return service.create(teamId, req);
    }

    @Operation(summary = "Listar os convites de uma equipe",
               description = "Só quem administra vê. Os tokens não voltam aqui — apenas o estado de "
                           + "cada convite.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convites do mais recente ao mais antigo"),
            @ApiResponse(responseCode = "403", description = "Você não administra esta equipe"),
            @ApiResponse(responseCode = "404", description = "Equipe não encontrada")
    })
    @GetMapping("/api/teams/{teamId}/invites")
    public List<InviteResponse> list(@Parameter(description = "Id da equipe") @PathVariable Long teamId) {
        return service.list(teamId);
    }

    @Operation(summary = "Revogar um convite",
               description = "Invalida o link imediatamente, mesmo que ainda tenha usos e prazo. "
                           + "Quem já entrou continua na equipe.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Convite revogado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Você não administra esta equipe"),
            @ApiResponse(responseCode = "404", description = "Convite não encontrado")
    })
    @DeleteMapping("/api/invites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@Parameter(description = "Id do convite") @PathVariable Long id) {
        service.revoke(id);
    }

    @Operation(summary = "Espiar um convite antes de aceitar",
               description = "Mostra para qual equipe o convite leva, quem o criou e com que papel "
                           + "você entra, sem consumi-lo. É o que uma tela de \"Você foi convidado "
                           + "para...\" chamaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do convite"),
            @ApiResponse(responseCode = "404", description = "Convite inválido, expirado, revogado ou sem usos")
    })
    @PostMapping("/api/invites/preview")
    public InvitePreviewResponse preview(@Valid @RequestBody TokenRequest req) {
        return service.preview(req.token());
    }

    @Operation(summary = "Aceitar um convite",
               description = "Consome um uso e adiciona você à equipe. Chamar de novo depois de já "
                           + "ser membro é inofensivo: devolve a equipe sem gastar outro uso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Você entrou na equipe"),
            @ApiResponse(responseCode = "404", description = "Convite inválido, expirado, revogado ou sem usos")
    })
    @PostMapping("/api/invites/accept")
    public TeamResponse accept(@Valid @RequestBody TokenRequest req) {
        return service.accept(req.token());
    }
}
