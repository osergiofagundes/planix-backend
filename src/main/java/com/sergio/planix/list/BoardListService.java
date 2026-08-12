package com.sergio.planix.list;

import com.sergio.planix.board.Board;
import com.sergio.planix.board.BoardAccess;
import com.sergio.planix.board.BoardRepository;
import com.sergio.planix.common.exception.NotFoundException;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.list.dto.BoardListResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BoardListService {

    private final BoardListRepository repo;
    private final BoardRepository boardRepo;
    private final BoardAccess access;

    public BoardListService(BoardListRepository repo, BoardRepository boardRepo, BoardAccess access) {
        this.repo = repo;
        this.boardRepo = boardRepo;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<BoardListResponse> listByBoard(Long boardId) {
        access.requireMember(boardId);
        return repo.findByBoardIdOrderByPositionAsc(boardId).stream().map(BoardListResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BoardListResponse get(Long id) {
        return BoardListResponse.from(findOrThrow(id));
    }

    public BoardListResponse create(Long boardId, BoardListRequest req) {
        access.requireMember(boardId);
        Board board = boardRepo.findById(boardId)
                .orElseThrow(() -> new NotFoundException("Quadro %d não encontrado".formatted(boardId)));
        int position = repo.countByBoardId(boardId);
        BoardList saved = repo.save(new BoardList(board, req.name(), position));
        return BoardListResponse.from(saved);
    }

    public BoardListResponse update(Long id, BoardListRequest req) {
        BoardList list = findOrThrow(id);
        list.setName(req.name());
        return BoardListResponse.from(list);
    }

    public void delete(Long id) {
        BoardList list = findOrThrow(id);
        Long boardId = list.getBoard().getId();

        repo.delete(list);
        repo.flush();
        reindex(boardId);
    }

    public void move(Long listId, int newPosition) {
        BoardList list = findOrThrow(listId);
        Long boardId = list.getBoard().getId();

        List<BoardList> siblings = repo.findByBoardIdOrderByPositionAsc(boardId);
        siblings.remove(list);
        int target = Math.max(0, Math.min(newPosition, siblings.size()));
        siblings.add(target, list);

        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setPosition(i);
        }
    }

    private void reindex(Long boardId) {
        List<BoardList> siblings = repo.findByBoardIdOrderByPositionAsc(boardId);
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setPosition(i);
        }
    }

    private BoardList findOrThrow(Long id) {
        BoardList list = repo.findById(id).orElseThrow(() -> naoEncontrada(id));
        if (!access.isMember(list.getBoard().getId())) {
            throw naoEncontrada(id);
        }
        return list;
    }

    private NotFoundException naoEncontrada(Long id) {
        return new NotFoundException("Lista %d não encontrada".formatted(id));
    }
}
