package com.sergio.planix.team;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.common.exception.ForbiddenException;
import com.sergio.planix.common.exception.NotFoundException;
import org.springframework.stereotype.Component;

@Component
public class TeamAccess {

    private final TeamMemberRepository memberRepo;
    private final CurrentUser currentUser;

    public TeamAccess(TeamMemberRepository memberRepo, CurrentUser currentUser) {
        this.memberRepo = memberRepo;
        this.currentUser = currentUser;
    }

    public boolean isMember(Long teamId) {
        return memberRepo.existsByTeamIdAndUserId(teamId, currentUser.id());
    }

    public TeamRole role(Long teamId) {
        return memberRepo.findByTeamIdAndUserId(teamId, currentUser.id())
                .map(TeamMember::getRole)
                .orElseThrow(() -> naoEncontrada(teamId));
    }

    public void requireMember(Long teamId) {
        if (!isMember(teamId)) {
            throw naoEncontrada(teamId);
        }
    }

    public void requireAdmin(Long teamId) {
        if (!role(teamId).isAdmin()) {
            throw new ForbiddenException("Apenas quem administra a equipe pode fazer isto");
        }
    }

    public void requireOwner(Long teamId) {
        if (role(teamId) != TeamRole.OWNER) {
            throw new ForbiddenException("Apenas o dono da equipe pode fazer isto");
        }
    }

    private NotFoundException naoEncontrada(Long teamId) {
        return new NotFoundException("Equipe %d não encontrada".formatted(teamId));
    }
}
