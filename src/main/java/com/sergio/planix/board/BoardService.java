package com.sergio.planix.board;

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

    public BoardService(BoardRepository repo, BoardListRepository listRepo) {
        this.repo = repo;
        this.listRepo = listRepo;
    }

    @Transactional(readOnly = true)
    public List<BoardResponse> list() {
        return repo.findAll().stream().map(BoardResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BoardResponse get(Long id) {
        return BoardResponse.from(findOrThrow(id));
    }

    public BoardResponse create(BoardRequest req) {
        Board saved = repo.save(new Board(req.name(), req.description()));
        return BoardResponse.from(saved);
    }

    public BoardResponse update(Long id, BoardRequest req) {
        Board board = findOrThrow(id);
        board.setName(req.name());
        board.setDescription(req.description());
        return BoardResponse.from(board);
    }

    public void delete(Long id, String confirmationName) {
        Board board = findOrThrow(id);
        // Regra: se o quadro tem conteúdo, exige o nome digitado como confirmação.
        // Basta olhar as listas — os cartões (e o resto) pendem delas.
        boolean hasContent = listRepo.existsByBoardId(id);
        if (hasContent && !board.getName().equals(confirmationName)) {
            throw new BoardNotEmptyException(
                    "O quadro possui conteúdo. Para excluir, confirme digitando o nome exato do quadro.");
        }
        repo.delete(board);   // cascade no banco remove listas/cartões/etc.
    }

    private Board findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Quadro %d não encontrado".formatted(id)));
    }
}