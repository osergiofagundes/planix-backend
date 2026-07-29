package com.sergio.planix.checklist;

import com.sergio.planix.checklist.dto.ChecklistItemRequest;
import com.sergio.planix.checklist.dto.ChecklistItemResponse;
import com.sergio.planix.common.dto.MoveRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ChecklistItemController {

    private final ChecklistItemService service;

    public ChecklistItemController(ChecklistItemService service) { this.service = service; }

    @GetMapping("/cards/{cardId}/checklist")
    public List<ChecklistItemResponse> list(@PathVariable Long cardId) {
        return service.listByCard(cardId);
    }

    @PostMapping("/cards/{cardId}/checklist")
    public ResponseEntity<ChecklistItemResponse> create(@PathVariable Long cardId,
                                                        @Valid @RequestBody ChecklistItemRequest req) {
        ChecklistItemResponse created = service.create(cardId, req);
        return ResponseEntity
                .created(URI.create("/api/checklist-items/" + created.id()))
                .body(created);
    }

    @PutMapping("/checklist-items/{id}")
    public ChecklistItemResponse update(@PathVariable Long id,
                                        @Valid @RequestBody ChecklistItemRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/checklist-items/{id}/toggle")
    public ChecklistItemResponse toggle(@PathVariable Long id) { return service.toggle(id); }

    @PatchMapping("/checklist-items/{id}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void move(@PathVariable Long id, @Valid @RequestBody MoveRequest req) {
        service.move(id, req.position());
    }

    @DeleteMapping("/checklist-items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
