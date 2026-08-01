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

        assertThatThrownBy(() -> authService.refresh(antigo))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThatThrownBy(() -> authService.refresh(atual))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshDesconhecido_naoFunciona() {
        assertThatThrownBy(() -> authService.refresh("nunca-foi-emitido"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    private AuthResponse registrar() {
        return authService.register(new RegisterRequest(
                "Teste", "user-" + UUID.randomUUID() + "@planix.test", "senha-de-teste"));
    }
}
