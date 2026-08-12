package com.sergio.planix.support;

import com.sergio.planix.auth.User;
import com.sergio.planix.auth.UserRepository;
import com.sergio.planix.board.BoardVisibility;
import com.sergio.planix.board.dto.BoardCreateRequest;
import com.sergio.planix.team.Team;
import com.sergio.planix.team.TeamMemberRepository;
import com.sergio.planix.team.TeamProvisioning;
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
    @Autowired protected TeamProvisioning teamProvisioning;
    @Autowired protected TeamMemberRepository teamMemberRepo;

    protected User usuarioLogado;

    protected Team equipeDoTeste;

    @BeforeEach
    void autenticaUsuarioDeTeste() {
        usuarioLogado = criarUsuario();
        equipeDoTeste = equipeDe(usuarioLogado);
        autenticarComo(usuarioLogado);
    }

    @AfterEach
    void limpaContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    protected User criarUsuario() {
        String email = "user-" + UUID.randomUUID() + "@planix.test";
        User user = userRepo.save(new User("Teste", email, encoder.encode("senha-de-teste")));
        teamProvisioning.createFirstTeamFor(user);
        return user;
    }

    protected Team equipeDe(User user) {
        return teamMemberRepo.findMembershipsOf(user.getId()).getFirst().getTeam();
    }

    protected BoardCreateRequest quadroAberto(String nome) {
        return quadroAberto(nome, null);
    }

    protected BoardCreateRequest quadroAberto(String nome, String descricao) {
        return new BoardCreateRequest(equipeDoTeste.getId(), nome, descricao, null,
                BoardVisibility.TEAM);
    }

    protected BoardCreateRequest quadroAbertoDe(User dono, String nome) {
        return new BoardCreateRequest(equipeDe(dono).getId(), nome, null, null,
                BoardVisibility.TEAM);
    }

    protected BoardCreateRequest quadroFechado(String nome) {
        return new BoardCreateRequest(equipeDoTeste.getId(), nome, null, null,
                BoardVisibility.RESTRICTED);
    }

    protected void autenticarComo(User user) {
        var auth = UsernamePasswordAuthenticationToken.authenticated(user.getId(), null, List.of());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }
}
