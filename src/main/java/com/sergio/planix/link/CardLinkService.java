package com.sergio.planix.link;

import com.sergio.planix.board.BoardAccess;
import com.sergio.planix.card.Card;
import com.sergio.planix.card.CardAccess;
import com.sergio.planix.common.exception.NotFoundException;
import com.sergio.planix.link.dto.CardLinkRequest;
import com.sergio.planix.link.dto.CardLinkResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CardLinkService {

    private final CardLinkRepository repo;
    private final CardAccess cardAccess;
    private final BoardAccess boardAccess;

    public CardLinkService(CardLinkRepository repo, CardAccess cardAccess, BoardAccess boardAccess) {
        this.repo = repo;
        this.cardAccess = cardAccess;
        this.boardAccess = boardAccess;
    }

    @Transactional(readOnly = true)
    public List<CardLinkResponse> listByCard(Long cardId) {
        cardAccess.require(cardId);
        return repo.findByCardIdOrderByCreatedAtDesc(cardId).stream().map(CardLinkResponse::from).toList();
    }

    public CardLinkResponse create(Long cardId, CardLinkRequest req) {
        Card card = cardAccess.require(cardId);
        return CardLinkResponse.from(repo.save(new CardLink(card, req.url(), req.title())));
    }

    public CardLinkResponse update(Long id, CardLinkRequest req) {
        CardLink link = findOrThrow(id);
        link.setUrl(req.url());
        link.setTitle(req.title());
        return CardLinkResponse.from(link);
    }

    public void delete(Long id) {
        repo.delete(findOrThrow(id));
    }

    private CardLink findOrThrow(Long id) {
        CardLink link = repo.findById(id).orElseThrow(() -> naoEncontrado(id));
        if (!boardAccess.isMember(link.getCard().getList().getBoard().getId())) {
            throw naoEncontrado(id);
        }
        return link;
    }

    private NotFoundException naoEncontrado(Long id) {
        return new NotFoundException("Link %d não encontrado".formatted(id));
    }
}
