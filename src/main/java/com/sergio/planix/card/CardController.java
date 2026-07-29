package com.sergio.planix.card;

import com.sergio.planix.card.dto.*;
import com.sergio.planix.history.dto.CardChangeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CardController {

    private final CardService service;

    public CardController(CardService service) { this.service = service; }

    @GetMapping("/lists/{listId}/cards")
    public List<CardResponse> list(@PathVariable Long listId) {
        return service.listByList(listId);
    }

    @PostMapping("/lists/{listId}/cards")
    public ResponseEntity<CardResponse> create(@PathVariable Long listId,
                                               @Valid @RequestBody CardCreateRequest req) {
        CardResponse created = service.create(listId, req);
        return ResponseEntity
                .created(URI.create("/api/cards/" + created.id()))
                .body(created);
    }

    @GetMapping("/cards/{id}")
    public CardResponse get(@PathVariable Long id) { return service.get(id); }

    @PutMapping("/cards/{id}")
    public CardResponse update(@PathVariable Long id, @Valid @RequestBody CardUpdateRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/cards/{id}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void move(@PathVariable Long id, @Valid @RequestBody CardMoveRequest req) {
        service.move(id, req.targetListId(), req.position());
    }

    @PatchMapping("/cards/{id}/complete")
    public CardResponse complete(@PathVariable Long id, @Valid @RequestBody CardCompleteRequest req) {
        return service.setCompleted(id, req.completed());
    }

    @DeleteMapping("/cards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }

    @GetMapping("/cards/{cardId}/changes")
    public List<CardChangeResponse> changes(@PathVariable Long cardId) {
        return service.listChanges(cardId);
    }
}
