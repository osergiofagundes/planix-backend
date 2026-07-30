package com.sergio.planix.member;

import com.sergio.planix.auth.dto.UserSummary;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/members")
public class BoardMemberController {

    private final BoardMemberService service;

    public BoardMemberController(BoardMemberService service) { this.service = service; }

    @GetMapping
    public List<UserSummary> list(@PathVariable Long boardId) {
        return service.list(boardId);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long boardId) {
        service.leave(boardId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long boardId, @PathVariable Long userId) {
        service.remove(boardId, userId);
    }
}
