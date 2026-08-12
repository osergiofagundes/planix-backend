package com.sergio.planix.board;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.User;
import com.sergio.planix.auth.UserRepository;
import com.sergio.planix.board.dto.BoardCreateRequest;
import com.sergio.planix.board.dto.BoardRequest;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.common.exception.BoardNotEmptyException;
import com.sergio.planix.common.exception.NotFoundException;
import com.sergio.planix.list.BoardListRepository;
import com.sergio.planix.team.TeamAccess;
import com.sergio.planix.team.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BoardService {

    private final BoardRepository repo;
    private final BoardListRepository listRepo;
    private final BoardMemberRepository memberRepo;
    private final TeamRepository teamRepo;
    private final UserRepository userRepo;
    private final BoardAccess access;
    private final TeamAccess teamAccess;
    private final CurrentUser currentUser;

    public BoardService(BoardRepository repo, BoardListRepository listRepo,
                        BoardMemberRepository memberRepo, TeamRepository teamRepo,
                        UserRepository userRepo, BoardAccess access, TeamAccess teamAccess,
                        CurrentUser currentUser) {
        this.repo = repo;
        this.listRepo = listRepo;
        this.memberRepo = memberRepo;
        this.teamRepo = teamRepo;
        this.userRepo = userRepo;
        this.access = access;
        this.teamAccess = teamAccess;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<BoardResponse> list(Long teamId) {
        List<Board> boards = teamId == null
                ? repo.findAccessibleBy(currentUser.id())
                : repo.findAccessibleIn(teamId, currentUser.id());
        return boards.stream().map(BoardResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BoardResponse get(Long id) {
        return BoardResponse.from(findOrThrow(id));
    }

    public BoardResponse create(BoardCreateRequest req) {
        teamAccess.requireMember(req.teamId());

        User me = currentUser.reference();
        Board saved = repo.save(new Board(teamRepo.getReferenceById(req.teamId()), me, req.name(),
                req.description(), req.icon(), req.visibilityOrDefault()));

        memberRepo.save(new BoardMember(saved, me));
        return BoardResponse.from(saved);
    }

    public BoardResponse update(Long id, BoardRequest req) {
        access.requireManager(id);
        Board board = findOrThrow(id);
        board.setName(req.name());
        board.setDescription(req.description());
        board.setIcon(req.icon());
        board.setVisibility(req.visibilityOrDefault());
        return BoardResponse.from(board);
    }

    public BoardResponse transferOwnership(Long id, Long newOwnerId) {
        access.requireManager(id);
        Board board = findOrThrow(id);
        if (!access.isMember(id, newOwnerId)) {
            throw new NotFoundException(
                    "Usuário %d não tem acesso ao quadro %d".formatted(newOwnerId, id));
        }
        board.setOwner(userRepo.getReferenceById(newOwnerId));
        return BoardResponse.from(board);
    }

    public void delete(Long id, String confirmationName) {
        access.requireManager(id);
        Board board = findOrThrow(id);

        boolean hasContent = listRepo.existsByBoardId(id);
        if (hasContent && !board.getName().equals(confirmationName)) {
            throw new BoardNotEmptyException(
                    "O quadro possui conteúdo. Para excluir, confirme digitando o nome exato do quadro.");
        }
        repo.delete(board);
    }

    private Board findOrThrow(Long id) {
        access.requireMember(id);
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Quadro %d não encontrado".formatted(id)));
    }
}
