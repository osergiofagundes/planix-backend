package com.sergio.planix.board;

import com.sergio.planix.board.dto.BoardRequest;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.board.dto.OwnerTransferRequest;
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
@RequestMapping("/api/boards")
@Tag(name = "Quadros")
public class BoardController {

    private final BoardService service;

    public BoardController(BoardService service) { this.service = service; }

    @Operation(summary = "Listar meus quadros",
               description = "Os quadros de que você participa — como dono ou como convidado.")
    @ApiResponse(responseCode = "200", description = "Lista de quadros (vazia se não há nenhum)")
    @GetMapping
    public List<BoardResponse> list() { return service.list(); }

    @Operation(summary = "Buscar um quadro")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quadro encontrado"),
            @ApiResponse(responseCode = "404", description = "Quadro não existe ou você não participa dele")
    })
    @GetMapping("/{id}")
    public BoardResponse get(@Parameter(description = "Id do quadro") @PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "Criar um quadro",
               description = "Quem cria vira dono e já entra como membro. A URL do novo quadro "
                           + "volta no header `Location`.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Quadro criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping
    public ResponseEntity<BoardResponse> create(@Valid @RequestBody BoardRequest req) {
        BoardResponse created = service.create(req);
        return ResponseEntity
                .created(URI.create("/api/boards/" + created.id()))
                .body(created);
    }

    @Operation(summary = "Renomear ou redescrever um quadro", description = "Só o dono pode.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quadro atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado")
    })
    @PutMapping("/{id}")
    public BoardResponse update(@Parameter(description = "Id do quadro") @PathVariable Long id,
                                @Valid @RequestBody BoardRequest req) {
        return service.update(id, req);
    }

    @Operation(summary = "Transferir a propriedade do quadro",
               description = "Só o dono atual pode transferir, e só para alguém que já é membro do "
                           + "quadro. Depois disso você continua membro, mas perde os poderes de dono.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quadro com o novo dono"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado, ou o novo dono não é membro dele")
    })
    @PatchMapping("/{id}/owner")
    public BoardResponse transferOwnership(@Parameter(description = "Id do quadro") @PathVariable Long id,
                                           @Valid @RequestBody OwnerTransferRequest req) {
        return service.transferOwnership(id, req.userId());
    }

    @Operation(summary = "Excluir um quadro",
               description = """
                       Só o dono pode, e a exclusão leva junto listas, cartões e tudo o que pende deles.

                       Se o quadro **tiver listas**, é preciso confirmar repetindo o nome exato do
                       quadro em `confirmationName`. Quadro vazio não pede confirmação.""")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Quadro excluído", content = @Content),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado"),
            @ApiResponse(responseCode = "409", description = "Quadro tem conteúdo e o nome de confirmação não bate")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Id do quadro") @PathVariable Long id,
                       @Parameter(description = "Nome exato do quadro. Obrigatório apenas quando o quadro não está vazio.")
                       @RequestParam(required = false) String confirmationName) {
        service.delete(id, confirmationName);
    }
}
