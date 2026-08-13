package com.sergio.planix.comment;

import com.sergio.planix.auth.User;
import com.sergio.planix.board.BoardService;
import com.sergio.planix.board.dto.BoardCreateRequest;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.card.CardService;
import com.sergio.planix.card.dto.CardCreateRequest;
import com.sergio.planix.card.dto.CardResponse;
import com.sergio.planix.comment.dto.CommentReactionRequest;
import com.sergio.planix.comment.dto.CommentReactionSummary;
import com.sergio.planix.comment.dto.CommentRequest;
import com.sergio.planix.comment.dto.CommentResponse;
import com.sergio.planix.common.exception.CommentDeletedException;
import com.sergio.planix.common.exception.NotFoundException;
import com.sergio.planix.invite.TeamInviteService;
import com.sergio.planix.invite.dto.InviteCreatedResponse;
import com.sergio.planix.invite.dto.InviteRequest;
import com.sergio.planix.list.BoardListService;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.list.dto.BoardListResponse;
import com.sergio.planix.support.AuthenticatedIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentThreadFlowIT extends AuthenticatedIntegrationTest {

    @Autowired BoardService boardService;
    @Autowired BoardListService listService;
    @Autowired CardService cardService;
    @Autowired CommentService commentService;
    @Autowired TeamInviteService inviteService;

    @Test
    void responderUmaResposta_ficaNaMesmaRaizDaThread() {
        CardResponse card = cartaoEm("Threads");

        CommentResponse raiz = comentar(card, "Precisamos renovar o domínio", null);
        CommentResponse resposta = comentar(card, "Já cotei, dá R$ 80", raiz.id());
        CommentResponse respostaDaResposta = comentar(card, "@Teste fecha então", resposta.id());

        assertThat(resposta.parentId()).isEqualTo(raiz.id());
        assertThat(respostaDaResposta.parentId()).isEqualTo(raiz.id());

        assertThat(commentService.listByCard(card.id()))
                .singleElement()
                .satisfies(thread -> {
                    assertThat(thread.id()).isEqualTo(raiz.id());
                    assertThat(thread.replies())
                            .extracting(CommentResponse::text)
                            .containsExactly("Já cotei, dá R$ 80", "@Teste fecha então");
                });
    }

    @Test
    void reagirDuasVezesComOMesmoEmoji_removeAReacao() {
        CardResponse card = cartaoEm("Reações");
        CommentResponse comentario = comentar(card, "Fechado", null);

        assertThat(reagir(comentario, "👍"))
                .singleElement()
                .satisfies(reacao -> {
                    assertThat(reacao.count()).isEqualTo(1);
                    assertThat(reacao.reactedByMe()).isTrue();
                    assertThat(reacao.users()).singleElement()
                            .satisfies(quem -> assertThat(quem.id()).isEqualTo(usuarioLogado.getId()));
                });

        assertThat(reagir(comentario, "🚀")).hasSize(2);
        assertThat(reagir(comentario, "👍")).singleElement()
                .satisfies(reacao -> assertThat(reacao.emoji()).isEqualTo("🚀"));
    }

    @Test
    void reacaoDeOutraPessoa_contaJuntoMasNaoMarcaReactedByMe() {
        BoardResponse board = boardService.create(quadroAberto("Reações compartilhadas"));
        BoardListResponse lista = listService.create(board.id(), new BoardListRequest("A Fazer"));
        CardResponse card = cardService.create(lista.id(), new CardCreateRequest("Comprar domínio"));

        CommentResponse comentario = comentar(card, "Fechado", null);
        reagir(comentario, "👍");

        autenticarComo(membroDoQuadro(board));
        assertThat(reagir(comentario, "👍"))
                .singleElement()
                .satisfies(reacao -> {
                    assertThat(reacao.count()).isEqualTo(2);
                    assertThat(reacao.reactedByMe()).isTrue();
                });

        autenticarComo(usuarioLogado);
        assertThat(commentService.listByCard(card.id()).getFirst().reactions())
                .singleElement()
                .satisfies(reacao -> {
                    assertThat(reacao.count()).isEqualTo(2);
                    assertThat(reacao.reactedByMe()).isTrue();
                });
    }

    @Test
    void excluirRaizComResposta_mantemAutorEReacoesEPerdeSoOTexto() {
        CardResponse card = cartaoEm("Exclusão com resposta");

        CommentResponse raiz = comentar(card, "Precisamos renovar o domínio", null);
        comentar(card, "Já cotei, dá R$ 80", raiz.id());
        reagir(raiz, "👍");

        commentService.delete(raiz.id());

        assertThat(commentService.listByCard(card.id()))
                .singleElement()
                .satisfies(lapide -> {
                    assertThat(lapide.deleted()).isTrue();
                    assertThat(lapide.text()).isNull();
                    assertThat(lapide.author().id()).isEqualTo(usuarioLogado.getId());
                    assertThat(lapide.reactions())
                            .extracting(CommentReactionSummary::emoji)
                            .containsExactly("👍");
                    assertThat(lapide.replies())
                            .extracting(CommentResponse::text)
                            .containsExactly("Já cotei, dá R$ 80");
                });
    }

    @Test
    void excluirComentarioSemResposta_continuaNaListaComoLapide() {
        CardResponse card = cartaoEm("Exclusão simples");
        CommentResponse comentario = comentar(card, "Ignorem", null);

        commentService.delete(comentario.id());

        assertThat(commentService.listByCard(card.id()))
                .singleElement()
                .satisfies(lapide -> {
                    assertThat(lapide.id()).isEqualTo(comentario.id());
                    assertThat(lapide.deleted()).isTrue();
                    assertThat(lapide.text()).isNull();
                    assertThat(lapide.author().id()).isEqualTo(usuarioLogado.getId());
                });
    }

    @Test
    void reagirEmComentarioExcluido_funciona() {
        CardResponse card = cartaoEm("Reagir na lápide");
        CommentResponse comentario = comentar(card, "Ignorem", null);
        commentService.delete(comentario.id());

        assertThat(reagir(comentario, "👍"))
                .singleElement()
                .satisfies(reacao -> {
                    assertThat(reacao.emoji()).isEqualTo("👍");
                    assertThat(reacao.reactedByMe()).isTrue();
                });

        assertThat(reagir(comentario, "👍")).isEmpty();
    }

    @Test
    void editarOuResponderComentarioExcluido_retornaConflito() {
        CardResponse card = cartaoEm("Excluído é imutável");
        CommentResponse raiz = comentar(card, "Precisamos renovar o domínio", null);
        comentar(card, "Já cotei", raiz.id());
        commentService.delete(raiz.id());

        assertThatThrownBy(() -> commentService.update(raiz.id(), new CommentRequest("Voltei", null)))
                .isInstanceOf(CommentDeletedException.class);

        assertThatThrownBy(() -> comentar(card, "Ainda dá?", raiz.id()))
                .isInstanceOf(CommentDeletedException.class);
    }

    @Test
    void editarUmComentario_marcaComoEditadoMasExcluirNao() {
        CardResponse card = cartaoEm("Marca de edição");
        CommentResponse comentario = comentar(card, "Vamos renovar", null);

        assertThat(comentario.edited()).isFalse();
        assertThat(commentService.listByCard(card.id()).getFirst().edited()).isFalse();

        commentService.update(comentario.id(), new CommentRequest("Vamos renovar já", null));
        assertThat(commentService.listByCard(card.id()).getFirst().edited()).isTrue();

        commentService.delete(comentario.id());
        assertThat(commentService.listByCard(card.id()).getFirst().edited()).isFalse();
    }

    @Test
    void responderComentarioDeQuadroAlheio_retorna404() {
        CardResponse meuCard = cartaoEm("Meu quadro");

        User estranho = criarUsuario();
        autenticarComo(estranho);
        CardResponse cardAlheio = cartaoEm(quadroAbertoDe(estranho, "Quadro do estranho"));
        CommentResponse comentarioAlheio = comentar(cardAlheio, "Segredo", null);

        autenticarComo(usuarioLogado);
        assertThatThrownBy(() ->
                commentService.create(meuCard.id(), new CommentRequest("Puxando conversa", comentarioAlheio.id())))
                .isInstanceOf(NotFoundException.class);

        assertThatThrownBy(() -> reagir(comentarioAlheio, "👀"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void responderComentarioDeOutroCartao_retorna404() {
        CardResponse card = cartaoEm("Dois cartões");
        BoardListResponse outraLista = listService.create(
                boardService.create(quadroAberto("Vizinho")).id(), new BoardListRequest("A Fazer"));
        CardResponse outroCard = cardService.create(outraLista.id(), new CardCreateRequest("Outro assunto"));

        CommentResponse comentarioDoOutro = comentar(outroCard, "Nada a ver", null);

        assertThatThrownBy(() ->
                commentService.create(card.id(), new CommentRequest("Resposta cruzada", comentarioDoOutro.id())))
                .isInstanceOf(NotFoundException.class);
    }

    private CardResponse cartaoEm(String nomeDoQuadro) {
        return cartaoEm(quadroAberto(nomeDoQuadro));
    }

    private CardResponse cartaoEm(BoardCreateRequest quadro) {
        BoardResponse board = boardService.create(quadro);
        BoardListResponse lista = listService.create(board.id(), new BoardListRequest("A Fazer"));
        return cardService.create(lista.id(), new CardCreateRequest("Comprar domínio"));
    }

    private CommentResponse comentar(CardResponse card, String texto, Long parentId) {
        return commentService.create(card.id(), new CommentRequest(texto, parentId));
    }

    private List<CommentReactionSummary> reagir(CommentResponse comentario, String emoji) {
        return commentService.toggleReaction(comentario.id(), new CommentReactionRequest(emoji));
    }

    /** Um segundo membro do quadro, para exercitar `reactedByMe` por conta. */
    private User membroDoQuadro(BoardResponse quadro) {
        InviteCreatedResponse convite =
                inviteService.create(quadro.teamId(), new InviteRequest(null, 1, null));
        User colega = criarUsuario();
        autenticarComo(colega);
        inviteService.accept(convite.token());
        autenticarComo(usuarioLogado);
        return colega;
    }
}
