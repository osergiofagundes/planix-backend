package com.sergio.planix.board;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.User;
import com.sergio.planix.auth.UserRepository;
import com.sergio.planix.board.dto.BoardRequest;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.common.BoardNotEmptyException;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.list.BoardListRepository;
import com.sergio.planix.member.BoardMember;
import com.sergio.planix.member.BoardMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BoardService {

    private final BoardRepository repo;
    private final BoardListRepository listRepo;
    private final BoardMemberRepository memberRepo;
    private final UserRepository userRepo;
    private final BoardAccess access;
    private final CurrentUser currentUser;

    public BoardService(BoardRepository repo, BoardListRepository listRepo,
                        BoardMemberRepository memberRepo, UserRepository userRepo,
                        BoardAccess access, CurrentUser currentUser) {
        this.repo = repo;
        this.listRepo = listRepo;
        this.memberRepo = memberRepo;
        this.userRepo = userRepo;
        this.access = access;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<BoardResponse> list() {
        return repo.findAccessibleBy(currentUser.id()).stream().map(BoardResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BoardResponse get(Long id) {
        return BoardResponse.from(findOrThrow(id));
    }

    public BoardResponse create(BoardRequest req) {
        User me = currentUser.reference();
        Board saved = repo.save(new Board(me, req.name(), req.description(), req.icon()));
        memberRepo.save(new BoardMember(saved, me));
        return BoardResponse.from(saved);
    }

    public BoardResponse update(Long id, BoardRequest req) {
        access.requireOwner(id);
        Board board = findOrThrow(id);
        board.setName(req.name());
        board.setDescription(req.description());
        board.setIcon(req.icon());
        return BoardResponse.from(board);
    }

    public BoardResponse transferOwnership(Long id, Long newOwnerId) {
        access.requireOwner(id);
        Board board = findOrThrow(id);
        if (!memberRepo.existsByBoardIdAndUserId(id, newOwnerId)) {
            throw new NotFoundException(
                    "Usuário %d não é membro do quadro %d".formatted(newOwnerId, id));
        }
        board.setOwner(userRepo.getReferenceById(newOwnerId));
        return BoardResponse.from(board);
    }

    public void delete(Long id, String confirmationName) {
        access.requireOwner(id);
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
