package com.sergio.planix.auth;

import com.sergio.planix.auth.dto.AuthResponse;
import com.sergio.planix.auth.dto.EmailChangeRequest;
import com.sergio.planix.auth.dto.PasswordChangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Conta")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) { this.service = service; }

    @Operation(summary = "Trocar meu e-mail",
               description = """
                       Troca o e-mail usado para entrar. A senha atual vai no corpo porque o token
                       sozinho não basta para mexer numa credencial.

                       Todas as sessões da conta são encerradas e a resposta traz um par de tokens
                       novo — guarde-o, senão este dispositivo também cai no próximo refresh.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "E-mail trocado; par de tokens novo"),
            @ApiResponse(responseCode = "400",
                         description = "E-mail inválido ou senha atual incorreta (veja `fieldErrors`)"),
            @ApiResponse(responseCode = "409", description = "O e-mail já pertence a outra conta")
    })
    @PutMapping("/email")
    public AuthResponse changeEmail(@Valid @RequestBody EmailChangeRequest req) {
        return service.changeEmail(req);
    }

    @Operation(summary = "Trocar minha senha",
               description = """
                       Exige a senha atual e uma nova diferente dela.

                       Todas as sessões da conta são encerradas — é justamente o que se espera de uma
                       troca de senha — e a resposta traz um par de tokens novo para este
                       dispositivo continuar logado.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha trocada; par de tokens novo"),
            @ApiResponse(responseCode = "400",
                         description = "Senha atual incorreta, senha nova curta demais ou igual à "
                                     + "atual (veja `fieldErrors`)")
    })
    @PutMapping("/password")
    public AuthResponse changePassword(@Valid @RequestBody PasswordChangeRequest req) {
        return service.changePassword(req);
    }
}
