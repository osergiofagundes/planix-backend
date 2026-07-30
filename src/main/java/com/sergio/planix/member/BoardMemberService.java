package com.sergio.planix.member;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.board.BoardAccess;
import com.sergio.planix.board.BoardRepository;
import com.sergio.planix.common.ForbiddenException;
import com.sergio.planix.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BoardMemberService {

    private final BoardMemberRepository memberRepo;
    private final BoardRepository boardRepo;
    private final BoardAccess access;
    private final CurrentUser currentUser;

    public BoardMemberService(BoardMemberRepository memberRepo, BoardRepository boardRepo,
                              BoardAccess access, CurrentUser currentUser) {
        this.memberRepo = memberRepo;
        this.boardRepo = boardRepo;
        this.access = access;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<UserSummary> list(Long boardId) {
        access.requireMember(boardId);          // qualquer membro vê quem mais está no quadro
        return memberRepo.findByBoardIdOrderByCreatedAtAsc(boardId).stream()
                .map(m -> UserSummary.from(m.getUser())).toList();
    }

    public void remove(Long boardId, Long userId) {
        access.requireOwner(boardId);
        if (boardRepo.existsByIdAndOwnerId(boardId, userId)) {
            throw new ForbiddenException("O dono não pode ser removido do próprio quadro");
        }
        BoardMember member = memberRepo.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Usuário %d não é membro do quadro %d".formatted(userId, boardId)));
        memberRepo.delete(member);
    }

    public void leave(Long boardId) {
        access.requireMember(boardId);
        if (boardRepo.existsByIdAndOwnerId(boardId, currentUser.id())) {
            throw new ForbiddenException(
                    "O dono não pode sair do próprio quadro. Exclua o quadro, se for o caso.");
        }
        memberRepo.deleteByBoardIdAndUserId(boardId, currentUser.id());
    }
}
