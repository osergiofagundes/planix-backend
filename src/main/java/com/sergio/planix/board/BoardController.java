package com.sergio.planix.board;

import com.sergio.planix.board.dto.BoardCreateRequest;
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
               description = """
                       Os quadros a que você tem acesso, em ordem alfabética: os que são seus, os
                       em que foi adicionado e os abertos às equipes de que participa.

                       Use `teamId` para ver só os de uma equipe.""")
    @ApiResponse(responseCode = "200", description = "Lista de quadros (vazia se não há nenhum)")
    @GetMapping
    public List<BoardResponse> list(
            @Parameter(description = "Filtra pelos quadros de uma equipe. Opcional.")
            @RequestParam(required = false) Long teamId) {
        return service.list(teamId);
    }

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
               description = """
                       O quadro nasce dentro de uma equipe de que você participa. Quem cria vira
                       dono e já entra como membro.

                       Por padrão o quadro é `TEAM`, isto é, aberto a toda a equipe; use
                       `visibility: "RESTRICTED"` para que só quem você adicionar tenha acesso.

                       A URL do novo quadro volta no header `Location`.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Quadro criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Equipe não encontrada ou você não participa dela")
    })
    @PostMapping
    public ResponseEntity<BoardResponse> create(@Valid @RequestBody BoardCreateRequest req) {
        BoardResponse created = service.create(req);
        return ResponseEntity
                .created(URI.create("/api/boards/" + created.id()))
                .body(created);
    }

    @Operation(summary = "Renomear, redescrever ou mudar a visibilidade de um quadro",
               description = "O dono do quadro e quem administra a equipe podem. Fechar um quadro "
                           + "(`RESTRICTED`) tira o acesso de quem só entrava por ser da equipe.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quadro atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Você não administra este quadro"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado")
    })
    @PutMapping("/{id}")
    public BoardResponse update(@Parameter(description = "Id do quadro") @PathVariable Long id,
                                @Valid @RequestBody BoardRequest req) {
        return service.update(id, req);
    }

    @Operation(summary = "Transferir a propriedade do quadro",
               description = "O dono atual e quem administra a equipe podem transferir, e só para "
                           + "alguém que já tem acesso ao quadro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quadro com o novo dono"),
            @ApiResponse(responseCode = "403", description = "Você não administra este quadro"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado, ou o novo dono não tem acesso a ele")
    })
    @PatchMapping("/{id}/owner")
    public BoardResponse transferOwnership(@Parameter(description = "Id do quadro") @PathVariable Long id,
                                           @Valid @RequestBody OwnerTransferRequest req) {
        return service.transferOwnership(id, req.userId());
    }

    @Operation(summary = "Excluir um quadro",
               description = """
                       O dono do quadro e quem administra a equipe podem, e a exclusão leva junto
                       listas, cartões e tudo o que pende deles.

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
