package com.sergio.planix.checklist;

import com.sergio.planix.checklist.dto.ChecklistItemRequest;
import com.sergio.planix.checklist.dto.ChecklistItemResponse;
import com.sergio.planix.common.dto.MoveRequest;
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
@Tag(name = "Checklist")
public class ChecklistItemController {

    private final ChecklistItemService service;

    public ChecklistItemController(ChecklistItemService service) { this.service = service; }

    @Operation(summary = "Listar os itens da checklist de um cartão",
               description = "Ordenados por `position`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Itens da checklist"),
            @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
    })
    @GetMapping("/cards/{cardId}/checklist")
    public List<ChecklistItemResponse> list(@Parameter(description = "Id do cartão") @PathVariable Long cardId) {
        return service.listByCard(cardId);
    }

    @Operation(summary = "Adicionar um item à checklist",
               description = "O item entra no fim da lista, desmarcado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
    })
    @PostMapping("/cards/{cardId}/checklist")
    public ResponseEntity<ChecklistItemResponse> create(@Parameter(description = "Id do cartão") @PathVariable Long cardId,
                                                        @Valid @RequestBody ChecklistItemRequest req) {
        ChecklistItemResponse created = service.create(cardId, req);
        return ResponseEntity
                .created(URI.create("/api/checklist-items/" + created.id()))
                .body(created);
    }

    @Operation(summary = "Editar o texto de um item")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Item não encontrado")
    })
    @PutMapping("/checklist-items/{id}")
    public ChecklistItemResponse update(@Parameter(description = "Id do item") @PathVariable Long id,
                                        @Valid @RequestBody ChecklistItemRequest req) {
        return service.update(id, req);
    }

    @Operation(summary = "Marcar ou desmarcar um item",
               description = "Inverte o estado atual — não precisa mandar corpo. Devolve o item já atualizado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item com o estado invertido"),
            @ApiResponse(responseCode = "404", description = "Item não encontrado")
    })
    @PatchMapping("/checklist-items/{id}/toggle")
    public ChecklistItemResponse toggle(@Parameter(description = "Id do item") @PathVariable Long id) {
        return service.toggle(id);
    }

    @Operation(summary = "Reordenar um item na checklist",
               description = "`position` é o índice de destino, começando em 0.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item reposicionado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Posição inválida"),
            @ApiResponse(responseCode = "404", description = "Item não encontrado")
    })
    @PatchMapping("/checklist-items/{id}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void move(@Parameter(description = "Id do item") @PathVariable Long id,
                     @Valid @RequestBody MoveRequest req) {
        service.move(id, req.position());
    }

    @Operation(summary = "Excluir um item da checklist")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item excluído", content = @Content),
            @ApiResponse(responseCode = "404", description = "Item não encontrado")
    })
    @DeleteMapping("/checklist-items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Id do item") @PathVariable Long id) {
        service.delete(id);
    }
}
