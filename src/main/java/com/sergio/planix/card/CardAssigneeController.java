package com.sergio.planix.card;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Responsáveis")
public class CardAssigneeController {

    private final CardAssigneeService service;

    public CardAssigneeController(CardAssigneeService service) { this.service = service; }

    @Operation(summary = "Atribuir um cartão a alguém",
               description = """
                       Só aceita quem já é membro do quadro. Atribuir **não concede acesso** — é uma
                       informação ("esta tarefa é sua"), da mesma natureza de uma etiqueta. Para dar
                       acesso a alguém de fora, a ferramenta é o convite.

                       Atribuir duas vezes a mesma pessoa é inofensivo: nada muda.""")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Responsável atribuído", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cartão não encontrado"),
            @ApiResponse(responseCode = "409", description = "Esse usuário não é membro do quadro")
    })
    @PostMapping("/cards/{cardId}/assignees/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assign(@Parameter(description = "Id do cartão") @PathVariable Long cardId,
                       @Parameter(description = "Id do usuário, que precisa ser membro do quadro") @PathVariable Long userId) {
        service.assign(cardId, userId);
    }

    @Operation(summary = "Tirar alguém da responsabilidade de um cartão",
               description = "Idempotente: se essa pessoa já não era responsável, a resposta é a mesma.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Responsável removido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
    })
    @DeleteMapping("/cards/{cardId}/assignees/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@Parameter(description = "Id do cartão") @PathVariable Long cardId,
                         @Parameter(description = "Id do usuário") @PathVariable Long userId) {
        service.unassign(cardId, userId);
    }
}
