package com.sergio.planix.auth;

import com.sergio.planix.auth.dto.EmailChangeRequest;
import com.sergio.planix.auth.dto.PasswordChangeRequest;
import com.sergio.planix.common.EmailAlreadyUsedException;
import com.sergio.planix.common.InvalidFieldException;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountFlowIT extends AuthenticatedIntegrationTest {

    private static final String SENHA_ATUAL = "senha-de-teste";   // a que o AuthenticatedIntegrationTest usa
    private static final String SENHA_NOVA = "senha-nova-123";

    @Autowired AccountService accountService;

    @Test
    void trocaDeSenha_gravaOhashNovo_edevolveUmParDeTokens() {
        var tokens = accountService.changePassword(
                new PasswordChangeRequest(SENHA_ATUAL, SENHA_NOVA));

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();

        String hash = userRepo.findById(usuarioLogado.getId()).orElseThrow().getPasswordHash();
        assertThat(encoder.matches(SENHA_NOVA, hash)).isTrue();
        assertThat(encoder.matches(SENHA_ATUAL, hash)).isFalse();
    }

    @Test
    void trocaDeSenha_comSenhaAtualErrada_acusaOcampoCurrentPassword() {
        assertThatThrownBy(() -> accountService.changePassword(
                new PasswordChangeRequest("nao-e-a-minha-senha", SENHA_NOVA)))
                .isInstanceOf(InvalidFieldException.class)
                .extracting(ex -> ((InvalidFieldException) ex).getField())
                .isEqualTo("currentPassword");
    }

    @Test
    void trocaDeSenha_pelaMesmaSenha_acusaOcampoNewPassword() {
        assertThatThrownBy(() -> accountService.changePassword(
                new PasswordChangeRequest(SENHA_ATUAL, SENHA_ATUAL)))
                .isInstanceOf(InvalidFieldException.class)
                .extracting(ex -> ((InvalidFieldException) ex).getField())
                .isEqualTo("newPassword");
    }

    @Test
    void trocaDeEmail_guardaOenderecoNormalizado() {
        String sufixo = UUID.randomUUID().toString();

        accountService.changeEmail(
                new EmailChangeRequest("  Sergio-" + sufixo + "@Planix.TEST  ", SENHA_ATUAL));

        assertThat(userRepo.findById(usuarioLogado.getId()).orElseThrow().getEmail())
                .isEqualTo("sergio-" + sufixo + "@planix.test");
    }

    @Test
    void trocaDeEmail_comSenhaAtualErrada_acusaOcampoCurrentPassword() {
        assertThatThrownBy(() -> accountService.changeEmail(
                new EmailChangeRequest("outro@planix.test", "nao-e-a-minha-senha")))
                .isInstanceOf(InvalidFieldException.class)
                .extracting(ex -> ((InvalidFieldException) ex).getField())
                .isEqualTo("currentPassword");
    }

    @Test
    void trocaDeEmail_paraOdeOutraConta_naoPassa() {
        User outro = criarUsuario();

        assertThatThrownBy(() -> accountService.changeEmail(
                new EmailChangeRequest(outro.getEmail(), SENHA_ATUAL)))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void trocaDeEmail_peloProprioEndereco_naoDaConflito() {
        var tokens = accountService.changeEmail(
                new EmailChangeRequest(usuarioLogado.getEmail(), SENHA_ATUAL));

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(userRepo.findById(usuarioLogado.getId()).orElseThrow().getEmail())
                .isEqualTo(usuarioLogado.getEmail());
    }
}
