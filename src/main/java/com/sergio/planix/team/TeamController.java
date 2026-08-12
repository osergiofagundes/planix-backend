package com.sergio.planix.team;

import com.sergio.planix.board.dto.OwnerTransferRequest;
import com.sergio.planix.team.dto.TeamRequest;
import com.sergio.planix.team.dto.TeamResponse;
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
@RequestMapping("/api/teams")
@Tag(name = "Equipes")
public class TeamController {

    private final TeamService service;

    public TeamController(TeamService service) { this.service = service; }

    @Operation(summary = "Listar minhas equipes",
               description = "As equipes de que você participa, em ordem alfabética. Cada uma vem "
                           + "com o seu papel nela, em `myRole`.")
    @ApiResponse(responseCode = "200", description = "Lista de equipes (vazia se não há nenhuma)")
    @GetMapping
    public List<TeamResponse> list() { return service.list(); }

    @Operation(summary = "Buscar uma equipe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipe encontrada"),
            @ApiResponse(responseCode = "404", description = "Equipe não existe ou você não participa dela")
    })
    @GetMapping("/{id}")
    public TeamResponse get(@Parameter(description = "Id da equipe") @PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "Criar uma equipe",
               description = "Quem cria vira dono e já entra como `OWNER`. A URL da nova equipe "
                           + "volta no header `Location`.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Equipe criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody TeamRequest req) {
        TeamResponse created = service.create(req);
        return ResponseEntity
                .created(URI.create("/api/teams/" + created.id()))
                .body(created);
    }

    @Operation(summary = "Renomear ou redescrever uma equipe", description = "Dono e admins podem.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipe atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Você não administra esta equipe"),
            @ApiResponse(responseCode = "404", description = "Equipe não encontrada")
    })
    @PutMapping("/{id}")
    public TeamResponse update(@Parameter(description = "Id da equipe") @PathVariable Long id,
                               @Valid @RequestBody TeamRequest req) {
        return service.update(id, req);
    }

    @Operation(summary = "Transferir a posse da equipe",
               description = "Só o dono atual pode, e só para alguém que já é membro. Quem recebe "
                           + "vira `OWNER`; você fica `ADMIN`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipe com o novo dono"),
            @ApiResponse(responseCode = "403", description = "Você não é o dono da equipe"),
            @ApiResponse(responseCode = "404", description = "Equipe não encontrada, ou o novo dono não é membro dela")
    })
    @PatchMapping("/{id}/owner")
    public TeamResponse transferOwnership(@Parameter(description = "Id da equipe") @PathVariable Long id,
                                          @Valid @RequestBody OwnerTransferRequest req) {
        return service.transferOwnership(id, req.userId());
    }

    @Operation(summary = "Excluir uma equipe",
               description = """
                       Só o dono pode, e a exclusão leva junto **todos os quadros da equipe**, com
                       listas, cartões e o que pende deles.

                       Se a equipe **tiver quadros**, é preciso confirmar repetindo o nome exato da
                       equipe em `confirmationName`. Equipe vazia não pede confirmação.""")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Equipe excluída", content = @Content),
            @ApiResponse(responseCode = "403", description = "Você não é o dono da equipe"),
            @ApiResponse(responseCode = "404", description = "Equipe não encontrada"),
            @ApiResponse(responseCode = "409", description = "Equipe tem quadros e o nome de confirmação não bate")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Id da equipe") @PathVariable Long id,
                       @Parameter(description = "Nome exato da equipe. Obrigatório apenas quando a equipe tem quadros.")
                       @RequestParam(required = false) String confirmationName) {
        service.delete(id, confirmationName);
    }
}
