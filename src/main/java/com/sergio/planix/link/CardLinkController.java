package com.sergio.planix.link;

import com.sergio.planix.link.dto.CardLinkRequest;
import com.sergio.planix.link.dto.CardLinkResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CardLinkController {

    private final CardLinkService service;

    public CardLinkController(CardLinkService service) { this.service = service; }

    @GetMapping("/cards/{cardId}/links")
    public List<CardLinkResponse> list(@PathVariable Long cardId) {
        return service.listByCard(cardId);
    }

    @PostMapping("/cards/{cardId}/links")
    public ResponseEntity<CardLinkResponse> create(@PathVariable Long cardId,
                                                   @Valid @RequestBody CardLinkRequest req) {
        CardLinkResponse created = service.create(cardId, req);
        return ResponseEntity
                .created(URI.create("/api/links/" + created.id()))
                .body(created);
    }

    @PutMapping("/links/{id}")
    public CardLinkResponse update(@PathVariable Long id, @Valid @RequestBody CardLinkRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/links/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
