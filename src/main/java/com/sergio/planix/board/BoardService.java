package com.sergio.planix.board;

import com.sergio.planix.board.dto.BoardRequest;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.common.BoardNotEmptyException;
import com.sergio.planix.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BoardService {

    private final BoardRepository repo;

    public BoardService(BoardRepository repo) {
        this.repo = repo;
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
        repo.delete(board);

    }

    private Board findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Quadro %d não encontrado".formatted(id)));
    }
}