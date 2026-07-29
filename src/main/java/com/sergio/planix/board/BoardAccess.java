package com.sergio.planix.board;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.common.NotFoundException;
import org.springframework.stereotype.Component;

@Component
public class BoardAccess {

    private final BoardRepository boardRepo;
    private final CurrentUser currentUser;

    public BoardAccess(BoardRepository boardRepo, CurrentUser currentUser) {
        this.boardRepo = boardRepo;
        this.currentUser = currentUser;
    }

    public boolean isMember(Long boardId) {
        return boardRepo.existsByIdAndOwnerId(boardId, currentUser.id());
    }

    public void requireMember(Long boardId) {
        if (!isMember(boardId)) {
            throw new NotFoundException("Quadro %d não encontrado".formatted(boardId));
        }
    }

    public void requireOwner(Long boardId) {
        requireMember(boardId);
    }
}
