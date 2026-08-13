package com.sergio.planix.comment;

import com.sergio.planix.comment.dto.CommentReactionRequest;
import com.sergio.planix.comment.dto.CommentReactionSummary;
import com.sergio.planix.comment.dto.CommentRequest;
import com.sergio.planix.comment.dto.CommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Comentários")
public class CommentController {

    private final CommentService service;

    public CommentController(CommentService service) { this.service = service; }

    @Operation(summary = "Listar os comentários de um cartão",
               description = """
                       Só os comentários raiz, do mais novo para o mais antigo, cada um \
                       com suas respostas em ordem cronológica e suas reações agrupadas \
                       por emoji. Comentário excluído continua na lista com \
                       `deleted: true` e sem texto, mantendo autor, data e reações.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comentários do cartão"),
            @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
    })
    @GetMapping("/cards/{cardId}/comments")
    public List<CommentResponse> list(@Parameter(description = "Id do cartão") @PathVariable Long cardId) {
        return service.listByCard(cardId);
    }

    @Operation(summary = "Comentar em um cartão",
               description = """
                       O autor é a conta autenticada — não se manda no corpo. Com \
                       `parentId` o comentário vira resposta; responder a uma resposta \
                       é aceito e cai na raiz da mesma thread.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comentário criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Cartão ou comentário respondido não encontrado"),
            @ApiResponse(responseCode = "409", description = "O comentário respondido foi excluído")
    })
    @PostMapping("/cards/{cardId}/comments")
    public ResponseEntity<CommentResponse> create(@Parameter(description = "Id do cartão") @PathVariable Long cardId,
                                                  @Valid @RequestBody CommentRequest req) {
        CommentResponse created = service.create(cardId, req);
        return ResponseEntity
                .created(URI.create("/api/comments/" + created.id()))
                .body(created);
    }

    @Operation(summary = "Editar um comentário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comentário atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Só o autor edita o próprio comentário"),
            @ApiResponse(responseCode = "404", description = "Comentário não encontrado"),
            @ApiResponse(responseCode = "409", description = "O comentário foi excluído")
    })
    @PutMapping("/comments/{id}")
    public CommentResponse update(@Parameter(description = "Id do comentário") @PathVariable Long id,
                                  @Valid @RequestBody CommentRequest req) {
        return service.update(id, req);
    }

    @Operation(summary = "Excluir um comentário",
               description = """
                       O texto é apagado, mas a linha continua na listagem com \
                       `deleted: true`, mantendo autor, data e reações. Assim as \
                       respostas não ficam órfãs e a tela desenha a lápide no lugar.""")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Comentário excluído", content = @Content),
            @ApiResponse(responseCode = "403", description = "Não é o autor nem gestor do quadro"),
            @ApiResponse(responseCode = "404", description = "Comentário não encontrado")
    })
    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Id do comentário") @PathVariable Long id) {
        service.delete(id);
    }

    @Operation(summary = "Reagir a um comentário",
               description = """
                       Alterna: reagir com um emoji que a conta já usou naquele \
                       comentário remove a reação. Devolve as reações do comentário \
                       já agrupadas por emoji. Vale também em comentário excluído, \
                       cuja lápide continua mostrando as reações.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reações do comentário"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Comentário não encontrado")
    })
    @PostMapping("/comments/{id}/reactions")
    public List<CommentReactionSummary> toggleReaction(
            @Parameter(description = "Id do comentário") @PathVariable Long id,
            @Valid @RequestBody CommentReactionRequest req) {
        return service.toggleReaction(id, req);
    }
}
