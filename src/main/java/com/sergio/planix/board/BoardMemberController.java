package com.sergio.planix.board;

import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.board.dto.AddMemberRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
               description = "Qualquer um com acesso ao quadro pode ver a lista. Num quadro "
                           + "`TEAM`, ela é a equipe inteira; num `RESTRICTED`, só quem foi "
                           + "adicionado, na ordem de entrada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membros do quadro"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado")
    })
    @GetMapping
    public List<UserSummary> list(@Parameter(description = "Id do quadro") @PathVariable Long boardId) {
        return service.list(boardId);
    }

    @Operation(summary = "Quem ainda dá para adicionar",
               description = "Os membros da equipe que ainda não estão neste quadro. Num quadro "
                           + "`TEAM` a lista vem vazia — a equipe toda já tem acesso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Candidatos a membro do quadro"),
            @ApiResponse(responseCode = "403", description = "Você não administra este quadro"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado")
    })
    @GetMapping("/candidates")
    public List<UserSummary> candidates(@Parameter(description = "Id do quadro") @PathVariable Long boardId) {
        return service.candidates(boardId);
    }

    @Operation(summary = "Dar acesso ao quadro para alguém da equipe",
               description = "É assim que se decide quem enxerga um quadro `RESTRICTED`. A pessoa "
                           + "precisa já ser membro da equipe — quem entra na equipe é pelo "
                           + "convite dela. Chamar de novo para quem já está é inofensivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A pessoa agora tem acesso ao quadro"),
            @ApiResponse(responseCode = "403", description = "Você não administra este quadro"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado"),
            @ApiResponse(responseCode = "409", description = "O quadro é aberto à equipe, ou essa pessoa não é da equipe")
    })
    @PostMapping
    public UserSummary add(@Parameter(description = "Id do quadro") @PathVariable Long boardId,
                           @Valid @RequestBody AddMemberRequest req) {
        return service.add(boardId, req.userId());
    }

    @Operation(summary = "Sair do quadro",
               description = "Remove **você** de um quadro `RESTRICTED` e tira seu nome dos cartões "
                           + "em que era responsável. O dono não pode sair do próprio quadro, e de "
                           + "quadro aberto à equipe não há como sair sem sair da equipe.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Você saiu do quadro", content = @Content),
            @ApiResponse(responseCode = "403", description = "Você é o dono e não pode sair do próprio quadro"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado"),
            @ApiResponse(responseCode = "409", description = "O quadro é aberto à equipe")
    })
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@Parameter(description = "Id do quadro") @PathVariable Long boardId) {
        service.leave(boardId);
    }

    @Operation(summary = "Tirar o acesso de alguém ao quadro",
               description = "O dono do quadro e quem administra a equipe podem, e não dá para "
                           + "remover o dono. A pessoa também perde as atribuições que tinha nos "
                           + "cartões deste quadro.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membro removido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Você não administra o quadro, ou tentou remover o dono"),
            @ApiResponse(responseCode = "404", description = "Quadro não encontrado, ou esse usuário não é membro dele"),
            @ApiResponse(responseCode = "409", description = "O quadro é aberto à equipe")
    })
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@Parameter(description = "Id do quadro") @PathVariable Long boardId,
                       @Parameter(description = "Id do usuário a remover") @PathVariable Long userId) {
        service.remove(boardId, userId);
    }
}
