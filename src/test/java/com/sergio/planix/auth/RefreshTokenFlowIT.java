package com.sergio.planix.auth;

import com.sergio.planix.auth.dto.AuthResponse;
import com.sergio.planix.auth.dto.RegisterRequest;
import com.sergio.planix.common.InvalidRefreshTokenException;
import com.sergio.planix.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ciclo de vida do refresh token — a parte do capítulo 11 mais fácil de quebrar sem perceber.
 *
 * <p>Nível de service, sem contexto de segurança: {@code register}, {@code login}, {@code refresh} e
 * {@code logout} identificam o usuário pelo token apresentado, não pelo {@code CurrentUser}.
 *
 * <p>Nada aqui é {@code @Transactional} de propósito: cada chamada de service abre e fecha a sua
 * própria transação, que é a única forma de observar o efeito do
 * {@code noRollbackFor} de {@code AuthService.refresh}.
 */
class RefreshTokenFlowIT extends IntegrationTest {

    @Autowired AuthService authService;

    @Test
    void refreshRotacionaOToken_eONovoValeAteOLogout() {
        AuthResponse inicial = registrar();

        AuthResponse rotacionado = authService.refresh(inicial.refreshToken());

        assertThat(rotacionado.refreshToken()).isNotEqualTo(inicial.refreshToken());
        assertThat(rotacionado.accessToken()).isNotBlank();

        authService.logout(rotacionado.refreshToken());

        assertThatThrownBy(() -> authService.refresh(rotacionado.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshJaRotacionadoReapresentado_revogaTodosOsTokensDoUsuario() {
        String antigo = registrar().refreshToken();
        String atual = authService.refresh(antigo).refreshToken();

        // reapresentar um refresh já queimado é sinal de token roubado: derruba a sessão inteira
        assertThatThrownBy(() -> authService.refresh(antigo))
                .isInstanceOf(InvalidRefreshTokenException.class);

        // e o token novo, que estava perfeitamente válido, também morre — é o que prova que o
        // revokeAllOf sobreviveu à exceção. Sem o noRollbackFor, a revogação voltaria atrás no
        // rollback e esta linha passaria batido: a defesa existiria só no código.
        assertThatThrownBy(() -> authService.refresh(atual))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshDesconhecido_naoFunciona() {
        assertThatThrownBy(() -> authService.refresh("nunca-foi-emitido"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    /** E-mail único: os testes não fazem rollback e uq_users_email é real. */
    private AuthResponse registrar() {
        return authService.register(new RegisterRequest(
                "Teste", "user-" + UUID.randomUUID() + "@planix.test", "senha-de-teste"));
    }
}
