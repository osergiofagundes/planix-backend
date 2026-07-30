package com.sergio.planix.invite;

import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.invite.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BoardInviteController {

    private final BoardInviteService service;

    public BoardInviteController(BoardInviteService service) { this.service = service; }

    @PostMapping("/api/boards/{boardId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteCreatedResponse create(@PathVariable Long boardId,
                                        @Valid @RequestBody InviteRequest req) {
        return service.create(boardId, req);
    }

    @GetMapping("/api/boards/{boardId}/invites")
    public List<InviteResponse> list(@PathVariable Long boardId) {
        return service.list(boardId);
    }

    @DeleteMapping("/api/invites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long id) {
        service.revoke(id);
    }

    @PostMapping("/api/invites/preview")
    public InvitePreviewResponse preview(@Valid @RequestBody TokenRequest req) {
        return service.preview(req.token());
    }

    @PostMapping("/api/invites/accept")
    public BoardResponse accept(@Valid @RequestBody TokenRequest req) {
        return service.accept(req.token());
    }
}
