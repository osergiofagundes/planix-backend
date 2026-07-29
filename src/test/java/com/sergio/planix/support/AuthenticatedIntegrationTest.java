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

/**
 * Base dos testes que chamam services diretamente: depois do capítulo 11 todos eles precisam de
 * um usuário no contexto de segurança, senão o CurrentUser.id() lança.
 */
public abstract class AuthenticatedIntegrationTest extends IntegrationTest {

    @Autowired protected UserRepository userRepo;
    @Autowired protected PasswordEncoder encoder;

    protected User usuarioLogado;

    @BeforeEach
    void autenticaUsuarioDeTeste() {
        usuarioLogado = criarUsuario();
        autenticarComo(usuarioLogado);
    }

    /**
     * O SecurityContextHolder guarda o usuário numa ThreadLocal, e o JUnit reaproveita threads
     * entre classes. Sem esta limpeza, um teste que deveria rodar sem autenticação herda o usuário
     * do anterior e passa por engano — o pior tipo de teste, o que dá verde errado.
     */
    @AfterEach
    void limpaContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    /** E-mail único: os testes não são @Transactional, nada é desfeito, e uq_users_email é real. */
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
