package com.sergio.planix.board;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.UserRepository;
import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.card.CardRepository;
import com.sergio.planix.common.exception.BoardOpenToTeamException;
import com.sergio.planix.common.exception.ForbiddenException;
import com.sergio.planix.common.exception.NotFoundException;
import com.sergio.planix.common.exception.NotTeamMemberException;
import com.sergio.planix.team.TeamMember;
import com.sergio.planix.team.TeamMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BoardMemberService {

    private final BoardMemberRepository memberRepo;
    private final BoardRepository boardRepo;
    private final TeamMemberRepository teamMemberRepo;
    private final CardRepository cardRepo;
    private final UserRepository userRepo;
    private final BoardAccess access;
    private final CurrentUser currentUser;

    public BoardMemberService(BoardMemberRepository memberRepo, BoardRepository boardRepo,
                              TeamMemberRepository teamMemberRepo, CardRepository cardRepo,
                              UserRepository userRepo, BoardAccess access, CurrentUser currentUser) {
        this.memberRepo = memberRepo;
        this.boardRepo = boardRepo;
        this.teamMemberRepo = teamMemberRepo;
        this.cardRepo = cardRepo;
        this.userRepo = userRepo;
        this.access = access;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<UserSummary> list(Long boardId) {
        access.requireMember(boardId);
        Board board = findBoardOrThrow(boardId);

        if (board.isOpenToTeam()) {
            return teamMemberRepo.findByTeamIdOrderByCreatedAtAsc(board.getTeam().getId()).stream()
                    .map(m -> UserSummary.from(m.getUser())).toList();
        }
        return memberRepo.findByBoardIdOrderByCreatedAtAsc(boardId).stream()
                .map(m -> UserSummary.from(m.getUser())).toList();
    }

    public UserSummary add(Long boardId, Long userId) {
        access.requireManager(boardId);
        Board board = findBoardOrThrow(boardId);

        if (board.isOpenToTeam()) {
            throw new BoardOpenToTeamException(
                    "Este quadro é aberto à equipe: todo mundo dela já tem acesso.");
        }
        if (!teamMemberRepo.existsByTeamIdAndUserId(board.getTeam().getId(), userId)) {
            throw new NotTeamMemberException(
                    "O usuário %d não é membro da equipe deste quadro".formatted(userId));
        }
        if (!memberRepo.existsByBoardIdAndUserId(boardId, userId)) {
            memberRepo.save(new BoardMember(board, userRepo.getReferenceById(userId)));
        }
        return UserSummary.from(userRepo.getReferenceById(userId));
    }

    public void remove(Long boardId, Long userId) {
        access.requireManager(boardId);
        Board board = findBoardOrThrow(boardId);

        if (board.isOpenToTeam()) {
            throw new BoardOpenToTeamException(
                    "Este quadro é aberto à equipe. Para tirar o acesso, feche o quadro ou "
                    + "remova a pessoa da equipe.");
        }
        if (boardRepo.existsByIdAndOwnerId(boardId, userId)) {
            throw new ForbiddenException("O dono não pode ser removido do próprio quadro");
        }
        BoardMember member = memberRepo.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Usuário %d não é membro do quadro %d".formatted(userId, boardId)));
        cardRepo.deleteAssigneesOfUserInBoard(boardId, userId);
        memberRepo.delete(member);
    }

    public void leave(Long boardId) {
        access.requireMember(boardId);
        Board board = findBoardOrThrow(boardId);

        if (board.isOpenToTeam()) {
            throw new BoardOpenToTeamException(
                    "Este quadro é aberto à equipe. Para sair dele, saia da equipe.");
        }
        if (boardRepo.existsByIdAndOwnerId(boardId, currentUser.id())) {
            throw new ForbiddenException(
                    "O dono não pode sair do próprio quadro. Exclua o quadro, se for o caso.");
        }
        cardRepo.deleteAssigneesOfUserInBoard(boardId, currentUser.id());
        memberRepo.deleteByBoardIdAndUserId(boardId, currentUser.id());
    }

    @Transactional(readOnly = true)
    public List<UserSummary> candidates(Long boardId) {
        access.requireManager(boardId);
        Board board = findBoardOrThrow(boardId);

        if (board.isOpenToTeam()) {
            return List.of();
        }
        return teamMemberRepo.findByTeamIdOrderByCreatedAtAsc(board.getTeam().getId()).stream()
                .map(TeamMember::getUser)
                .filter(u -> !memberRepo.existsByBoardIdAndUserId(boardId, u.getId()))
                .map(UserSummary::from).toList();
    }

    private Board findBoardOrThrow(Long boardId) {
        return boardRepo.findById(boardId)
                .orElseThrow(() -> new NotFoundException("Quadro %d não encontrado".formatted(boardId)));
    }
}
