package com.sergio.planix.auth;

import com.sergio.planix.board.BoardService;
import com.sergio.planix.board.dto.BoardRequest;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.card.CardService;
import com.sergio.planix.card.dto.CardCreateRequest;
import com.sergio.planix.card.dto.CardResponse;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.list.BoardListService;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.list.dto.BoardListResponse;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O teste de maior valor do projeto: <b>um usuário não vê os dados do outro</b>.
 *
 * <p>É a regra que, se quebrar, ninguém percebe até ser tarde — não há tela vermelha, não há
 * exceção no log, a API só responde 200 para quem não devia. Uma única linha de {@code requireMember}
 * apagada por engano tem que derrubar a build por causa daqui.
 *
 * <p>Todas as asserções esperam <b>404, nunca 403</b>: dizer "existe, mas não é seu" já é vazar
 * informação sobre o que existe no sistema de outra pessoa.
 */
class AuthFlowIT extends AuthenticatedIntegrationTest {

    @Autowired BoardService boardService;
    @Autowired BoardListService listService;
    @Autowired CardService cardService;

    @Test
    void quadroDeOutroUsuario_respondeNaoEncontrado() {
        BoardResponse meuQuadro = boardService.create(new BoardRequest("Meu quadro", null));

        autenticarComo(criarUsuario());          // agora somos outra pessoa

        assertThatThrownBy(() -> boardService.get(meuQuadro.id()))
                .isInstanceOf(NotFoundException.class);
        assertThat(boardService.list())
                .extracting(BoardResponse::id)
                .doesNotContain(meuQuadro.id());
    }

    @Test
    void cartaoDeOutroUsuario_respondeNaoEncontrado() {
        BoardResponse quadro = boardService.create(new BoardRequest("Quadro do dono", null));
        BoardListResponse lista = listService.create(quadro.id(), new BoardListRequest("A Fazer"));
        CardResponse cartao = cardService.create(lista.id(), new CardCreateRequest("Segredo"));

        autenticarComo(criarUsuario());

        assertThatThrownBy(() -> cardService.get(cartao.id()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> cardService.listByList(lista.id()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> cardService.listChanges(cartao.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void moverCartaoParaListaDeOutroUsuario_respondeNaoEncontrado() {
        BoardResponse quadroDoOutro = boardService.create(new BoardRequest("Quadro do outro", null));
        BoardListResponse listaDoOutro =
                listService.create(quadroDoOutro.id(), new BoardListRequest("Destino"));

        autenticarComo(criarUsuario());
        BoardResponse meuQuadro = boardService.create(new BoardRequest("Meu quadro", null));
        BoardListResponse minhaLista = listService.create(meuQuadro.id(), new BoardListRequest("A Fazer"));
        CardResponse meuCartao = cardService.create(minhaLista.id(), new CardCreateRequest("Meu cartão"));

        // a lista de destino nem existe do meu ponto de vista: 404, não 409 de quadro diferente
        assertThatThrownBy(() -> cardService.move(meuCartao.id(), listaDoOutro.id(), 0))
                .isInstanceOf(NotFoundException.class);

        assertThat(cardService.listByList(minhaLista.id()))
                .extracting(CardResponse::title)
                .containsExactly("Meu cartão");      // o cartão não saiu do lugar
    }
}
