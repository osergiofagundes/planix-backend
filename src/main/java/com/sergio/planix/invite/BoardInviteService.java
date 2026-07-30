package com.sergio.planix.invite;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.board.Board;
import com.sergio.planix.board.BoardAccess;
import com.sergio.planix.board.BoardRepository;
import com.sergio.planix.board.dto.BoardResponse;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.common.Tokens;
import com.sergio.planix.invite.dto.*;
import com.sergio.planix.member.BoardMember;
import com.sergio.planix.member.BoardMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class BoardInviteService {

    private static final int DIAS_PADRAO = 7;
    private static final int USOS_PADRAO = 1;

    private final BoardInviteRepository repo;
    private final BoardRepository boardRepo;
    private final BoardMemberRepository memberRepo;
    private final BoardAccess access;
    private final CurrentUser currentUser;

    public BoardInviteService(BoardInviteRepository repo, BoardRepository boardRepo,
                              BoardMemberRepository memberRepo, BoardAccess access,
                              CurrentUser currentUser) {
        this.repo = repo;
        this.boardRepo = boardRepo;
        this.memberRepo = memberRepo;
        this.access = access;
        this.currentUser = currentUser;
    }

    public InviteCreatedResponse create(Long boardId, InviteRequest req) {
        access.requireOwner(boardId);

        String value = Tokens.random();
        BoardInvite invite = repo.save(new BoardInvite(
                boardRepo.getReferenceById(boardId),
                currentUser.reference(),
                Tokens.sha256(value),
                req.maxUses() == null ? USOS_PADRAO : req.maxUses(),
                OffsetDateTime.now().plusDays(
                        req.expiresInDays() == null ? DIAS_PADRAO : req.expiresInDays())));

        return new InviteCreatedResponse(invite.getId(), value,     // o valor, só desta vez
                invite.getExpiresAt(), invite.getMaxUses());
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> list(Long boardId) {
        access.requireOwner(boardId);
        return repo.findByBoardIdOrderByCreatedAtDesc(boardId).stream()
                .map(InviteResponse::from).toList();
    }

    public void revoke(Long inviteId) {
        BoardInvite invite = repo.findById(inviteId)
                .orElseThrow(() -> new NotFoundException(
                        "Convite %d não encontrado".formatted(inviteId)));
        access.requireOwner(invite.getBoard().getId());
        invite.setRevokedAt(OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public InvitePreviewResponse preview(String presented) {
        BoardInvite invite = findUsableOrThrow(presented);
        return new InvitePreviewResponse(invite.getBoard().getName(),
                UserSummary.from(invite.getCreatedBy()), invite.getExpiresAt());
    }

    public BoardResponse accept(String presented) {
        BoardInvite invite = repo.findByTokenHash(Tokens.sha256(presented)).orElseThrow(this::invalido);
        Board board = invite.getBoard();

        if (memberRepo.existsByBoardIdAndUserId(board.getId(), currentUser.id())) {
            return BoardResponse.from(board);
        }
        if (!invite.isUsable() || repo.consume(invite.getId()) == 0) {   // ou alguém chegou primeiro
            throw invalido();
        }
        memberRepo.save(new BoardMember(board, currentUser.reference()));
        return BoardResponse.from(board);
    }

    private BoardInvite findUsableOrThrow(String presented) {
        return repo.findByTokenHash(Tokens.sha256(presented))
                .filter(BoardInvite::isUsable)
                .orElseThrow(this::invalido);
    }

    private NotFoundException invalido() {
        return new NotFoundException("Convite inválido ou expirado");
    }
}
