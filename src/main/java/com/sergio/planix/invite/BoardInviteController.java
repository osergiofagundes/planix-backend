package com.sergio.planix.invite;

import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.invite.dto.*;
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
public class BoardInviteController {

    private final BoardInviteService service;

    public BoardInviteController(BoardInviteService service) { this.service = service; }

    @Operation(summary = "Gerar um link de convite",
               description = """
                       Só o dono do quadro pode gerar. O token em texto claro aparece **apenas nesta
                       resposta** — o banco guarda só o hash, então não há como recuperá-lo depois.
                       Se perder, revogue e gere outro.

                       Padrões quando os campos são omitidos: 1 uso e 7 dias de validade.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Convite criado; o token vem em `token`"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado")
    })
    @PostMapping("/api/boards/{boardId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteCreatedResponse create(@Parameter(description = "Id do quadro") @PathVariable Long boardId,
                                        @Valid @RequestBody InviteRequest req) {
        return service.create(boardId, req);
    }

    @Operation(summary = "Listar os convites de um quadro",
               description = "Só o dono vê. Os tokens não voltam aqui — apenas o estado de cada convite.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convites do mais recente ao mais antigo"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado")
    })
    @GetMapping("/api/boards/{boardId}/invites")
    public List<InviteResponse> list(@Parameter(description = "Id do quadro") @PathVariable Long boardId) {
        return service.list(boardId);
    }

    @Operation(summary = "Revogar um convite",
               description = "Invalida o link imediatamente, mesmo que ainda tenha usos e prazo. "
                           + "Quem já entrou continua membro.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Convite revogado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Convite não encontrado")
    })
    @DeleteMapping("/api/invites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@Parameter(description = "Id do convite") @PathVariable Long id) {
        service.revoke(id);
    }

    @Operation(summary = "Espiar um convite antes de aceitar",
               description = "Mostra para qual quadro o convite leva e quem o criou, sem consumi-lo. "
                           + "É o que uma tela de \"Você foi convidado para...\" chamaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do convite"),
            @ApiResponse(responseCode = "404", description = "Convite inválido, expirado, revogado ou sem usos")
    })
    @PostMapping("/api/invites/preview")
    public InvitePreviewResponse preview(@Valid @RequestBody TokenRequest req) {
        return service.preview(req.token());
    }

    @Operation(summary = "Aceitar um convite",
               description = "Consome um uso e adiciona você ao quadro. Chamar de novo depois de já "
                           + "ser membro é inofensivo: devolve o quadro sem gastar outro uso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Você entrou no quadro"),
            @ApiResponse(responseCode = "404", description = "Convite inválido, expirado, revogado ou sem usos")
    })
    @PostMapping("/api/invites/accept")
    public BoardResponse accept(@Valid @RequestBody TokenRequest req) {
        return service.accept(req.token());
    }
}
