package com.sergio.planix.label;

import com.sergio.planix.label.dto.LabelRequest;
import com.sergio.planix.label.dto.LabelResponse;
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
@Tag(name = "Etiquetas")
public class LabelController {

    private final LabelService service;

    public LabelController(LabelService service) { this.service = service; }

    @Operation(summary = "Listar as etiquetas de um quadro",
               description = "A etiqueta pertence ao **quadro**, não ao cartão: você a cria uma vez "
                           + "e aplica em quantos cartões quiser.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Etiquetas do quadro"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado")
    })
    @GetMapping("/boards/{boardId}/labels")
    public List<LabelResponse> list(@Parameter(description = "Id do quadro") @PathVariable Long boardId) {
        return service.listByBoard(boardId);
    }

    @Operation(summary = "Criar uma etiqueta no quadro",
               description = "O nome é único dentro do quadro.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Etiqueta criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado"),
            @ApiResponse(responseCode = "409", description = "Já existe uma etiqueta com esse nome no quadro")
    })
    @PostMapping("/boards/{boardId}/labels")
    public ResponseEntity<LabelResponse> create(@Parameter(description = "Id do quadro") @PathVariable Long boardId,
                                                @Valid @RequestBody LabelRequest req) {
        LabelResponse created = service.create(boardId, req);
        return ResponseEntity
                .created(URI.create("/api/labels/" + created.id()))
                .body(created);
    }

    @Operation(summary = "Renomear ou recolorir uma etiqueta",
               description = "A mudança aparece de uma vez em todos os cartões que a usam.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Etiqueta atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Etiqueta não encontrada"),
            @ApiResponse(responseCode = "409", description = "Já existe outra etiqueta com esse nome no quadro")
    })
    @PutMapping("/labels/{id}")
    public LabelResponse update(@Parameter(description = "Id da etiqueta") @PathVariable Long id,
                                @Valid @RequestBody LabelRequest req) {
        return service.update(id, req);
    }

    @Operation(summary = "Excluir uma etiqueta",
               description = "Some de todos os cartões do quadro. Os cartões continuam intactos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Etiqueta excluída", content = @Content),
            @ApiResponse(responseCode = "404", description = "Etiqueta não encontrada")
    })
    @DeleteMapping("/labels/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Id da etiqueta") @PathVariable Long id) {
        service.delete(id);
    }

    @Operation(summary = "Aplicar uma etiqueta a um cartão",
               description = "Etiqueta e cartão precisam ser do mesmo quadro. Aplicar de novo é inofensivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Etiqueta aplicada", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cartão ou etiqueta não encontrados")
    })
    @PostMapping("/cards/{cardId}/labels/{labelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attach(@Parameter(description = "Id do cartão") @PathVariable Long cardId,
                       @Parameter(description = "Id da etiqueta") @PathVariable Long labelId) {
        service.attach(cardId, labelId);
    }

    @Operation(summary = "Tirar uma etiqueta de um cartão",
               description = "Só desfaz a ligação — a etiqueta continua existindo no quadro.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Etiqueta removida do cartão", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cartão ou etiqueta não encontrados")
    })
    @DeleteMapping("/cards/{cardId}/labels/{labelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detach(@Parameter(description = "Id do cartão") @PathVariable Long cardId,
                       @Parameter(description = "Id da etiqueta") @PathVariable Long labelId) {
        service.detach(cardId, labelId);
    }
}
