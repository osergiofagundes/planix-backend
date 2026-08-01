package com.sergio.planix.list;

import com.sergio.planix.common.dto.MoveRequest;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.list.dto.BoardListResponse;
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
@Tag(name = "Listas")
public class BoardListController {

    private final BoardListService service;

    public BoardListController(BoardListService service) { this.service = service; }

    @Operation(summary = "Listar as listas de um quadro",
               description = "Já vêm ordenadas por `position` — é a ordem em que apareceriam na tela.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listas do quadro"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado")
    })
    @GetMapping("/boards/{boardId}/lists")
    public List<BoardListResponse> list(@Parameter(description = "Id do quadro") @PathVariable Long boardId) {
        return service.listByBoard(boardId);
    }

    @Operation(summary = "Criar uma lista",
               description = "A lista nova entra no fim do quadro. A URL dela volta no header `Location`.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lista criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado")
    })
    @PostMapping("/boards/{boardId}/lists")
    public ResponseEntity<BoardListResponse> create(@Parameter(description = "Id do quadro") @PathVariable Long boardId,
                                                    @Valid @RequestBody BoardListRequest req) {
        BoardListResponse created = service.create(boardId, req);
        return ResponseEntity
                .created(URI.create("/api/lists/" + created.id()))
                .body(created);
    }

    @Operation(summary = "Buscar uma lista")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista encontrada"),
            @ApiResponse(responseCode = "404", description = "Lista não encontrada")
    })
    @GetMapping("/lists/{id}")
    public BoardListResponse get(@Parameter(description = "Id da lista") @PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "Renomear uma lista")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Lista não encontrada")
    })
    @PutMapping("/lists/{id}")
    public BoardListResponse update(@Parameter(description = "Id da lista") @PathVariable Long id,
                                    @Valid @RequestBody BoardListRequest req) {
        return service.update(id, req);
    }

    @Operation(summary = "Excluir uma lista",
               description = "Leva junto os cartões que estavam nela.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Lista excluída", content = @Content),
            @ApiResponse(responseCode = "404", description = "Lista não encontrada")
    })
    @DeleteMapping("/lists/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Id da lista") @PathVariable Long id) {
        service.delete(id);
    }

    @Operation(summary = "Reordenar uma lista dentro do quadro",
               description = "`position` é o índice de destino, começando em 0. As demais listas "
                           + "se acomodam sozinhas — você não precisa mandar as posições delas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Lista reposicionada", content = @Content),
            @ApiResponse(responseCode = "400", description = "Posição inválida"),
            @ApiResponse(responseCode = "404", description = "Lista não encontrada")
    })
    @PatchMapping("/lists/{id}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void move(@Parameter(description = "Id da lista") @PathVariable Long id,
                     @Valid @RequestBody MoveRequest req) {
        service.move(id, req.position());
    }
}
