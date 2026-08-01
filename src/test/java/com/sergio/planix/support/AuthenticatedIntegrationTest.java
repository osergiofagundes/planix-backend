package com.sergio.planix.support;

import com.sergio.planix.auth.User;
import com.sergio.planix.auth.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

public abstract class AuthenticatedIntegrationTest extends IntegrationTest {

    @Autowired protected UserRepository userRepo;
    @Autowired protected PasswordEncoder encoder;

    protected User usuarioLogado;

    @BeforeEach
    void autenticaUsuarioDeTeste() {
        usuarioLogado = criarUsuario();
        autenticarComo(usuarioLogado);
    }

    @AfterEach
    void limpaContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    protected User criarUsuario() {
        String email = "user-" + UUID.randomUUID() + "@planix.test";
        return userRepo.save(new User("Teste", email, encoder.encode("senha-de-teste")));
    }

    protected void autenticarComo(User user) {
        var auth = UsernamePasswordAuthenticationToken.authenticated(user.getId(), null, List.of());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }
}
