package com.sergio.planix.auth;

import com.sergio.planix.auth.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) { this.service = service; }

    @Operation(summary = "Criar uma conta",
               description = "Devolve o par de tokens já autenticado — não é preciso chamar o login em seguida.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (veja `fieldErrors`)"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    })
    @SecurityRequirements
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return service.register(req);
    }

    @Operation(summary = "Entrar",
               description = "E-mail inexistente e senha errada devolvem a mesma mensagem, "
                           + "de propósito: assim ninguém descobre quais e-mails existem.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos")
    })
    @SecurityRequirements
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req);
    }

    @Operation(summary = "Renovar o par de tokens",
               description = "Troca um refresh token válido por um par novo. O refresh usado é "
                           + "revogado no mesmo passo, então cada um serve uma vez só.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Par de tokens renovado"),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado ou já revogado")
    })
    @SecurityRequirements
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return service.refresh(req.refreshToken());
    }

    @Operation(summary = "Sair deste dispositivo",
               description = "Revoga o refresh token enviado. O access token atual continua válido "
                           + "até expirar — essa janela de até 15 minutos é o preço do stateless.")
    @ApiResponse(responseCode = "204", description = "Refresh token revogado", content = @Content)
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest req) {
        service.logout(req.refreshToken());
    }

    @Operation(summary = "Sair de todos os dispositivos",
               description = "Revoga todos os refresh tokens ativos da conta autenticada.")
    @ApiResponse(responseCode = "204", description = "Todos os refresh tokens revogados", content = @Content)
    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll() { service.logoutAll(); }

    @Operation(summary = "Quem sou eu",
               description = "Dados da conta dona do token enviado. Útil para conferir se o "
                           + "`Authorization` está sendo aceito.")
    @ApiResponse(responseCode = "200", description = "Dados da conta autenticada")
    @GetMapping("/me")
    public UserResponse me() { return service.me(); }
}
