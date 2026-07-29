package com.sergio.planix.board;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.board.dto.BoardRequest;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.common.BoardNotEmptyException;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.list.BoardListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BoardService {

    private final BoardRepository repo;
    private final BoardListRepository listRepo;
    private final BoardAccess access;
    private final CurrentUser currentUser;

    public BoardService(BoardRepository repo, BoardListRepository listRepo,
                        BoardAccess access, CurrentUser currentUser) {
        this.repo = repo;
        this.listRepo = listRepo;
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
        Board saved = repo.save(new Board(currentUser.reference(), req.name(), req.description()));
        return BoardResponse.from(saved);
    }

    public BoardResponse update(Long id, BoardRequest req) {
        access.requireOwner(id);
        Board board = findOrThrow(id);
        board.setName(req.name());
        board.setDescription(req.description());
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
