package com.sergio.planix.invite;

import com.sergio.planix.auth.User;
import com.sergio.planix.board.BoardService;
import com.sergio.planix.board.dto.BoardRequest;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.invite.dto.InviteCreatedResponse;
import com.sergio.planix.invite.dto.InviteRequest;
import com.sergio.planix.member.BoardMemberRepository;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class InviteConcurrencyIT extends AuthenticatedIntegrationTest {

    @Autowired BoardService boardService;
    @Autowired BoardInviteService inviteService;
    @Autowired BoardInviteRepository inviteRepo;
    @Autowired BoardMemberRepository memberRepo;

    @Test
    void doisAcceptsSimultaneosNumConviteDeUmUso_apenasUmEntra() throws Exception {
        BoardResponse quadro = boardService.create(new BoardRequest("Corrida", null));
        InviteCreatedResponse convite = inviteService.create(quadro.id(), new InviteRequest(null, 1));

        User b = criarUsuario();
        User c = criarUsuario();
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            List<Future<Boolean>> tentativas = new ArrayList<>();
            for (User quemTenta : List.of(b, c)) {
                tentativas.add(pool.submit(() -> {
                    autenticarComo(quemTenta);
                    try {
                        largada.await();
                        inviteService.accept(convite.token());
                        return true;
                    } catch (NotFoundException e) {
                        return false;
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }
            largada.countDown();

            List<Boolean> resultados = new ArrayList<>();
            for (Future<Boolean> tentativa : tentativas) {
                resultados.add(tentativa.get(30, TimeUnit.SECONDS));
            }
            assertThat(resultados).containsExactlyInAnyOrder(true, false);
        } finally {
            pool.shutdownNow();
        }

        assertThat(inviteRepo.findById(convite.id()).orElseThrow().getUses()).isEqualTo(1);
        assertThat(memberRepo.findByBoardIdOrderByCreatedAtAsc(quadro.id()))
                .hasSize(2);
    }
}
