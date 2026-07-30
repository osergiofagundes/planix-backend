package com.sergio.planix.board;

import com.sergio.planix.board.dto.BoardRequest;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.board.dto.OwnerTransferRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService service;

    public BoardController(BoardService service) { this.service = service; }

    @GetMapping
    public List<BoardResponse> list() { return service.list(); }

    @GetMapping("/{id}")
    public BoardResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    public ResponseEntity<BoardResponse> create(@Valid @RequestBody BoardRequest req) {
        BoardResponse created = service.create(req);
        return ResponseEntity
                .created(URI.create("/api/boards/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public BoardResponse update(@PathVariable Long id, @Valid @RequestBody BoardRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/owner")
    public BoardResponse transferOwnership(@PathVariable Long id,
                                           @Valid @RequestBody OwnerTransferRequest req) {
        return service.transferOwnership(id, req.userId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)   // 204
    public void delete(@PathVariable Long id,
                       @RequestParam(required = false) String confirmationName) {
        service.delete(id, confirmationName);
    }
}