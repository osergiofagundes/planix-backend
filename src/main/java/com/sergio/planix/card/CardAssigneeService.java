package com.sergio.planix.card;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.UserRepository;
import com.sergio.planix.common.NotBoardMemberException;
import com.sergio.planix.history.CardChange;
import com.sergio.planix.history.CardChangeRepository;
import com.sergio.planix.member.BoardMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CardAssigneeService {

    private static final String CAMPO = "assignee";

    private final CardAccess cardAccess;
    private final BoardMemberRepository memberRepo;
    private final UserRepository userRepo;
    private final CardChangeRepository changeRepo;
    private final CurrentUser currentUser;

    public CardAssigneeService(CardAccess cardAccess, BoardMemberRepository memberRepo,
                               UserRepository userRepo, CardChangeRepository changeRepo,
                               CurrentUser currentUser) {
        this.cardAccess = cardAccess;
        this.memberRepo = memberRepo;
        this.userRepo = userRepo;
        this.changeRepo = changeRepo;
        this.currentUser = currentUser;
    }

    public void assign(Long cardId, Long userId) {
        Card card = cardAccess.require(cardId);
        Long boardId = card.getList().getBoard().getId();

        if (!memberRepo.existsByBoardIdAndUserId(boardId, userId)) {
            throw new NotBoardMemberException(
                    "O usuário %d não é membro deste quadro".formatted(userId));
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
