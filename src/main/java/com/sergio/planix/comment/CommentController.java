package com.sergio.planix.comment;

import com.sergio.planix.comment.dto.CommentRequest;
import com.sergio.planix.comment.dto.CommentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService service;

    public CommentController(CommentService service) { this.service = service; }

    @GetMapping("/cards/{cardId}/comments")
    public List<CommentResponse> list(@PathVariable Long cardId) {
        return service.listByCard(cardId);
    }

    @PostMapping("/cards/{cardId}/comments")
    public ResponseEntity<CommentResponse> create(@PathVariable Long cardId,
                                                  @Valid @RequestBody CommentRequest req) {
        CommentResponse created = service.create(cardId, req);
        return ResponseEntity
                .created(URI.create("/api/comments/" + created.id()))
                .body(created);
    }

    @PutMapping("/comments/{id}")
    public CommentResponse update(@PathVariable Long id, @Valid @RequestBody CommentRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
