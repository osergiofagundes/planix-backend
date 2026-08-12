package com.sergio.planix.board;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.common.ForbiddenException;
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
        return boardRepo.hasAccess(boardId, currentUser.id());
    }

    public boolean isMember(Long boardId, Long userId) {
        return boardRepo.hasAccess(boardId, userId);
    }

    public void requireMember(Long boardId) {
        if (!isMember(boardId)) {
            throw new NotFoundException("Quadro %d não encontrado".formatted(boardId));
        }
    }

    public void requireManager(Long boardId) {
        requireMember(boardId);
        if (!boardRepo.canManage(boardId, currentUser.id())) {
            throw new ForbiddenException(
                    "Apenas o dono do quadro ou quem administra a equipe pode fazer isto");
        }
    }
}
