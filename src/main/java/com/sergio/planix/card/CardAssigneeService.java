package com.sergio.planix.card;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.UserRepository;
import com.sergio.planix.common.NotBoardMemberException;
import com.sergio.planix.history.CardChange;
import com.sergio.planix.history.CardChangeRepository;
import com.sergio.planix.board.BoardAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CardAssigneeService {

    private static final String CAMPO = "assignee";

    private final CardAccess cardAccess;
    private final BoardAccess boardAccess;
    private final UserRepository userRepo;
    private final CardChangeRepository changeRepo;
    private final CurrentUser currentUser;

    public CardAssigneeService(CardAccess cardAccess, BoardAccess boardAccess,
                               UserRepository userRepo, CardChangeRepository changeRepo,
                               CurrentUser currentUser) {
        this.cardAccess = cardAccess;
        this.boardAccess = boardAccess;
        this.userRepo = userRepo;
        this.changeRepo = changeRepo;
        this.currentUser = currentUser;
    }

    public void assign(Long cardId, Long userId) {
        Card card = cardAccess.require(cardId);
        Long boardId = card.getList().getBoard().getId();

        if (!boardAccess.isMember(boardId, userId)) {
            throw new NotBoardMemberException(
                    "O usuário %d não tem acesso a este quadro".formatted(userId));
        }
        if (card.getAssignees().add(userRepo.getReferenceById(userId))) {
            registrar(card, null, String.valueOf(userId));
        }
    }

    public void unassign(Long cardId, Long userId) {
        Card card = cardAccess.require(cardId);
        if (card.getAssignees().removeIf(u -> u.getId().equals(userId))) {
            registrar(card, String.valueOf(userId), null);
        }
    }

    private void registrar(Card card, String oldValue, String newValue) {
        changeRepo.save(new CardChange(card, currentUser.reference(), CAMPO, oldValue, newValue));
    }
}
