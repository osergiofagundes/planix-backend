package com.sergio.planix.board;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.common.ForbiddenException;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.member.BoardMemberRepository;
import org.springframework.stereotype.Component;

@Component
public class BoardAccess {

    private final BoardRepository boardRepo;
    private final BoardMemberRepository memberRepo;
    private final CurrentUser currentUser;

    public BoardAccess(BoardRepository boardRepo, BoardMemberRepository memberRepo,
                       CurrentUser currentUser) {
        this.boardRepo = boardRepo;
        this.memberRepo = memberRepo;
        this.currentUser = currentUser;
    }

    public boolean isMember(Long boardId) {
        return memberRepo.existsByBoardIdAndUserId(boardId, currentUser.id());
    }

    public void requireMember(Long boardId) {
        if (!isMember(boardId)) {
            throw new NotFoundException("Quadro %d não encontrado".formatted(boardId));
        }
    }

    public void requireOwner(Long boardId) {
        requireMember(boardId);
        if (!boardRepo.existsByIdAndOwnerId(boardId, currentUser.id())) {
            throw new ForbiddenException("Apenas o dono do quadro pode fazer isto");
        }
    }
}
