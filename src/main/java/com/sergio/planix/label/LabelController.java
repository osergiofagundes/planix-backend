package com.sergio.planix.label;

import com.sergio.planix.label.dto.LabelRequest;
import com.sergio.planix.label.dto.LabelResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class LabelController {

    private final LabelService service;

    public LabelController(LabelService service) { this.service = service; }

    @GetMapping("/boards/{boardId}/labels")
    public List<LabelResponse> list(@PathVariable Long boardId) {
        return service.listByBoard(boardId);
    }

    @PostMapping("/boards/{boardId}/labels")
    public ResponseEntity<LabelResponse> create(@PathVariable Long boardId,
                                                @Valid @RequestBody LabelRequest req) {
        LabelResponse created = service.create(boardId, req);
        return ResponseEntity
                .created(URI.create("/api/labels/" + created.id()))
                .body(created);
    }

    @PutMapping("/labels/{id}")
    public LabelResponse update(@PathVariable Long id, @Valid @RequestBody LabelRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/labels/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }

    @PostMapping("/cards/{cardId}/labels/{labelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attach(@PathVariable Long cardId, @PathVariable Long labelId) {
        service.attach(cardId, labelId);
    }

    @DeleteMapping("/cards/{cardId}/labels/{labelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detach(@PathVariable Long cardId, @PathVariable Long labelId) {
        service.detach(cardId, labelId);
    }
}
