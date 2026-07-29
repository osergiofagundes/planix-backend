package com.sergio.planix.card;

import com.sergio.planix.board.BoardAccess;
import com.sergio.planix.common.NotFoundException;
import org.springframework.stereotype.Component;

@Component
public class CardAccess {

    private final CardRepository cardRepo;
    private final BoardAccess boardAccess;

    public CardAccess(CardRepository cardRepo, BoardAccess boardAccess) {
        this.cardRepo = cardRepo;
        this.boardAccess = boardAccess;
    }

    public Card require(Long cardId) {
        Card card = cardRepo.findById(cardId).orElseThrow(() -> naoEncontrado(cardId));
        if (!boardAccess.isMember(card.getList().getBoard().getId())) {
            throw naoEncontrado(cardId);
        }
        return card;
    }

    private NotFoundException naoEncontrado(Long cardId) {
        return new NotFoundException("Cartão %d não encontrado".formatted(cardId));
    }
}
