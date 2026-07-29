package com.sergio.planix.list;

import com.sergio.planix.common.dto.MoveRequest;
import com.sergio.planix.list.dto.BoardListRequest;
import com.sergio.planix.list.dto.BoardListResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BoardListController {

    private final BoardListService service;

    public BoardListController(BoardListService service) { this.service = service; }

    @GetMapping("/boards/{boardId}/lists")
    public List<BoardListResponse> list(@PathVariable Long boardId) {
        return service.listByBoard(boardId);
    }

    @PostMapping("/boards/{boardId}/lists")
    public ResponseEntity<BoardListResponse> create(@PathVariable Long boardId,
                                                    @Valid @RequestBody BoardListRequest req) {
        BoardListResponse created = service.create(boardId, req);
        return ResponseEntity
                .created(URI.create("/api/lists/" + created.id()))
                .body(created);
    }

    @GetMapping("/lists/{id}")
    public BoardListResponse get(@PathVariable Long id) { return service.get(id); }

    @PutMapping("/lists/{id}")
    public BoardListResponse update(@PathVariable Long id, @Valid @RequestBody BoardListRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/lists/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }

    @PatchMapping("/lists/{id}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void move(@PathVariable Long id, @Valid @RequestBody MoveRequest req) {
        service.move(id, req.position());
    }
}
