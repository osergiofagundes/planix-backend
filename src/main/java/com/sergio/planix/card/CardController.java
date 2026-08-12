package com.sergio.planix.card;

import com.sergio.planix.card.dto.*;
import com.sergio.planix.card.dto.CardChangeResponse;
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
@Tag(name = "Cartões")
public class CardController {

    private final CardService service;

    public CardController(CardService service) { this.service = service; }

    @Operation(summary = "Listar os cartões de uma lista",
               description = "Ordenados por `position`. Cada cartão já vem com suas etiquetas e responsáveis.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cartões da lista"),
            @ApiResponse(responseCode = "404", description = "Lista não encontrada")
    })
    @GetMapping("/lists/{listId}/cards")
    public List<CardResponse> list(@Parameter(description = "Id da lista") @PathVariable Long listId) {
        return service.listByList(listId);
    }

    @Operation(summary = "Criar um cartão",
               description = "O cartão entra no fim da lista. Só o título é obrigatório; o resto "
                           + "(descrição, prazo, prioridade) pode vir depois por `PUT`.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cartão criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Lista não encontrada")
    })
    @PostMapping("/lists/{listId}/cards")
    public ResponseEntity<CardResponse> create(@Parameter(description = "Id da lista") @PathVariable Long listId,
                                               @Valid @RequestBody CardCreateRequest req) {
        CardResponse created = service.create(listId, req);
        return ResponseEntity
                .created(URI.create("/api/cards/" + created.id()))
                .body(created);
    }

    @Operation(summary = "Buscar um cartão")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cartão encontrado"),
            @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
    })
    @GetMapping("/cards/{id}")
    public CardResponse get(@Parameter(description = "Id do cartão") @PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "Atualizar um cartão",
               description = "Substitui os campos editáveis do cartão. Cada alteração vira uma "
                           + "linha no histórico — veja `GET /api/cards/{cardId}/changes`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cartão atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
    })
    @PutMapping("/cards/{id}")
    public CardResponse update(@Parameter(description = "Id do cartão") @PathVariable Long id,
                               @Valid @RequestBody CardUpdateRequest req) {
        return service.update(id, req);
    }

    @Operation(summary = "Mover um cartão",
               description = """
                       É o "arrastar cartão" do Trello. Serve tanto para reordenar dentro da mesma
                       lista quanto para trocá-lo de lista, desde que a lista de destino esteja **no
                       mesmo quadro** — mover entre quadros é recusado com 409.""")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cartão movido", content = @Content),
            @ApiResponse(responseCode = "400", description = "Posição inválida"),
            @ApiResponse(responseCode = "404", description = "Cartão ou lista de destino não encontrados"),
            @ApiResponse(responseCode = "409", description = "A lista de destino pertence a outro quadro")
    })
    @PatchMapping("/cards/{id}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void move(@Parameter(description = "Id do cartão") @PathVariable Long id,
                     @Valid @RequestBody CardMoveRequest req) {
        service.move(id, req.targetListId(), req.position());
    }

    @Operation(summary = "Concluir ou reabrir um cartão",
               description = "Concluir grava também o `completedAt`; reabrir o limpa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cartão com o novo estado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
    })
    @PatchMapping("/cards/{id}/complete")
    public CardResponse complete(@Parameter(description = "Id do cartão") @PathVariable Long id,
                                 @Valid @RequestBody CardCompleteRequest req) {
        return service.setCompleted(id, req.completed());
    }

    @Operation(summary = "Excluir um cartão",
               description = "Leva junto checklist, comentários, links, anexos e histórico.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cartão excluído", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
    })
    @DeleteMapping("/cards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Id do cartão") @PathVariable Long id) {
        service.delete(id);
    }

    @Operation(summary = "Histórico de alterações do cartão",
               description = "Uma linha por campo alterado: o que mudou, de que valor para qual, "
                           + "quando e por quem.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alterações do cartão"),
            @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
    })
    @GetMapping("/cards/{cardId}/changes")
    public List<CardChangeResponse> changes(@Parameter(description = "Id do cartão") @PathVariable Long cardId) {
        return service.listChanges(cardId);
    }
}
