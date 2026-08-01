package com.sergio.planix.member;

import com.sergio.planix.auth.dto.UserSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/members")
@Tag(name = "Membros do quadro")
public class BoardMemberController {

    private final BoardMemberService service;

    public BoardMemberController(BoardMemberService service) { this.service = service; }

    @Operation(summary = "Listar quem está no quadro",
               description = "Qualquer membro pode ver a lista, não só o dono. Vem na ordem de entrada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membros do quadro"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado")
    })
    @GetMapping
    public List<UserSummary> list(@Parameter(description = "Id do quadro") @PathVariable Long boardId) {
        return service.list(boardId);
    }

    @Operation(summary = "Sair do quadro",
               description = "Remove **você** do quadro e tira seu nome dos cartões em que era "
                           + "responsável. O dono não pode sair do próprio quadro — para ele a "
                           + "saída é transferir a propriedade ou excluir o quadro.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Você saiu do quadro", content = @Content),
            @ApiResponse(responseCode = "403", description = "Você é o dono e não pode sair do próprio quadro"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado")
    })
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@Parameter(description = "Id do quadro") @PathVariable Long boardId) {
        service.leave(boardId);
    }

    @Operation(summary = "Remover alguém do quadro",
               description = "Só o dono pode, e não pode remover a si mesmo. A pessoa removida "
                           + "também perde as atribuições que tinha nos cartões deste quadro.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membro removido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Tentativa de remover o dono do quadro"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado, ou esse usuário não é membro dele")
    })
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@Parameter(description = "Id do quadro") @PathVariable Long boardId,
                       @Parameter(description = "Id do usuário a remover") @PathVariable Long userId) {
        service.remove(boardId, userId);
    }
}
