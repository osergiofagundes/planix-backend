package com.sergio.planix.invite;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.common.ForbiddenException;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.common.Tokens;
import com.sergio.planix.invite.dto.*;
import com.sergio.planix.team.*;
import com.sergio.planix.team.dto.TeamResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class TeamInviteService {

    private static final int DIAS_PADRAO = 7;
    private static final int USOS_PADRAO = 1;

    private final TeamInviteRepository repo;
    private final TeamRepository teamRepo;
    private final TeamMemberRepository memberRepo;
    private final TeamAccess access;
    private final TeamResponses responses;
    private final CurrentUser currentUser;

    public TeamInviteService(TeamInviteRepository repo, TeamRepository teamRepo,
                             TeamMemberRepository memberRepo, TeamAccess access,
                             TeamResponses responses, CurrentUser currentUser) {
        this.repo = repo;
        this.teamRepo = teamRepo;
        this.memberRepo = memberRepo;
        this.access = access;
        this.responses = responses;
        this.currentUser = currentUser;
    }

    public InviteCreatedResponse create(Long teamId, InviteRequest req) {
        access.requireAdmin(teamId);

        TeamRole role = req.role() == null ? TeamRole.MEMBER : req.role();
        if (role == TeamRole.OWNER) {
            throw new ForbiddenException("Não dá para convidar alguém como dono da equipe");
        }

        String value = Tokens.random();
        TeamInvite invite = repo.save(new TeamInvite(
                teamRepo.getReferenceById(teamId),
                currentUser.reference(),
                Tokens.sha256(value),
                role,
                req.maxUses() == null ? USOS_PADRAO : req.maxUses(),
                OffsetDateTime.now().plusDays(
                        req.expiresInDays() == null ? DIAS_PADRAO : req.expiresInDays())));

        return new InviteCreatedResponse(invite.getId(), value,
                invite.getExpiresAt(), invite.getMaxUses());
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> list(Long teamId) {
        access.requireAdmin(teamId);
        return repo.findByTeamIdOrderByCreatedAtDesc(teamId).stream()
                .map(InviteResponse::from).toList();
    }

    public void revoke(Long inviteId) {
        TeamInvite invite = repo.findById(inviteId)
                .orElseThrow(() -> new NotFoundException(
                        "Convite %d não encontrado".formatted(inviteId)));
        access.requireAdmin(invite.getTeam().getId());
        invite.setRevokedAt(OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public InvitePreviewResponse preview(String presented) {
        TeamInvite invite = findUsableOrThrow(presented);
        return new InvitePreviewResponse(invite.getTeam().getName(),
                UserSummary.from(invite.getCreatedBy()), invite.getRole(), invite.getExpiresAt());
    }

    public TeamResponse accept(String presented) {
        TeamInvite invite = repo.findByTokenHash(Tokens.sha256(presented)).orElseThrow(this::invalido);
        Team team = invite.getTeam();

        TeamMember jaMembro = memberRepo.findByTeamIdAndUserId(team.getId(), currentUser.id())
                .orElse(null);
        if (jaMembro != null) {
            return responses.of(team, jaMembro.getRole());
        }
        if (!invite.isUsable() || repo.consume(invite.getId()) == 0) {
            throw invalido();
        }
        memberRepo.save(new TeamMember(team, currentUser.reference(), invite.getRole()));
        return responses.of(team, invite.getRole());
    }

    private TeamInvite findUsableOrThrow(String presented) {
        return repo.findByTokenHash(Tokens.sha256(presented))
                .filter(TeamInvite::isUsable)
                .orElseThrow(this::invalido);
    }

    private NotFoundException invalido() {
        return new NotFoundException("Convite inválido ou expirado");
    }
}
