package com.sergio.planix.link;

import com.sergio.planix.link.dto.CardLinkRequest;
import com.sergio.planix.link.dto.CardLinkResponse;
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
@Tag(name = "Links")
public class CardLinkController {

    private final CardLinkService service;

    public CardLinkController(CardLinkService service) { this.service = service; }

    @Operation(summary = "Listar os links de um cartão")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Links do cartão"),
            @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
    })
    @GetMapping("/cards/{cardId}/links")
    public List<CardLinkResponse> list(@Parameter(description = "Id do cartão") @PathVariable Long cardId) {
        return service.listByCard(cardId);
    }

    @Operation(summary = "Anexar um link a um cartão",
               description = "Guarda apenas a URL e um título — nada é baixado do endereço.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Link criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
    })
    @PostMapping("/cards/{cardId}/links")
    public ResponseEntity<CardLinkResponse> create(@Parameter(description = "Id do cartão") @PathVariable Long cardId,
                                                   @Valid @RequestBody CardLinkRequest req) {
        CardLinkResponse created = service.create(cardId, req);
        return ResponseEntity
                .created(URI.create("/api/links/" + created.id()))
                .body(created);
    }

    @Operation(summary = "Editar um link")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Link atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Link não encontrado")
    })
    @PutMapping("/links/{id}")
    public CardLinkResponse update(@Parameter(description = "Id do link") @PathVariable Long id,
                                   @Valid @RequestBody CardLinkRequest req) {
        return service.update(id, req);
    }

    @Operation(summary = "Excluir um link")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Link excluído", content = @Content),
            @ApiResponse(responseCode = "404", description = "Link não encontrado")
    })
    @DeleteMapping("/links/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Id do link") @PathVariable Long id) {
        service.delete(id);
    }
}
