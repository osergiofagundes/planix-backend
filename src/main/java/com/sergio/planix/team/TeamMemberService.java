package com.sergio.planix.team;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.card.CardRepository;
import com.sergio.planix.common.exception.ForbiddenException;
import com.sergio.planix.common.exception.NotFoundException;
import com.sergio.planix.board.BoardMemberRepository;
import com.sergio.planix.team.dto.TeamMemberResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TeamMemberService {

    private final TeamMemberRepository memberRepo;
    private final BoardMemberRepository boardMemberRepo;
    private final CardRepository cardRepo;
    private final TeamAccess access;
    private final CurrentUser currentUser;

    public TeamMemberService(TeamMemberRepository memberRepo, BoardMemberRepository boardMemberRepo,
                             CardRepository cardRepo, TeamAccess access, CurrentUser currentUser) {
        this.memberRepo = memberRepo;
        this.boardMemberRepo = boardMemberRepo;
        this.cardRepo = cardRepo;
        this.access = access;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> list(Long teamId) {
        access.requireMember(teamId);
        return memberRepo.findByTeamIdOrderByCreatedAtAsc(teamId).stream()
                .map(TeamMemberResponse::from).toList();
    }

    public TeamMemberResponse changeRole(Long teamId, Long userId, TeamRole role) {
        access.requireOwner(teamId);

        if (role == TeamRole.OWNER) {
            throw new ForbiddenException(
                    "Para tornar alguém dono da equipe, use a transferência de posse");
        }
        TeamMember member = findMemberOrThrow(teamId, userId);
        if (member.getRole() == TeamRole.OWNER) {
            throw new ForbiddenException(
                    "O dono não muda de papel. Transfira a posse da equipe primeiro.");
        }
        member.setRole(role);
        return TeamMemberResponse.from(member);
    }

    public void remove(Long teamId, Long userId) {
        TeamRole meuPapel = access.role(teamId);
        if (!meuPapel.isAdmin()) {
            throw new ForbiddenException("Apenas quem administra a equipe pode fazer isto");
        }

        TeamMember member = findMemberOrThrow(teamId, userId);
        if (member.getRole() == TeamRole.OWNER) {
            throw new ForbiddenException("O dono não pode ser removido da própria equipe");
        }
        if (member.getRole() == TeamRole.ADMIN && meuPapel != TeamRole.OWNER) {
            throw new ForbiddenException("Só o dono da equipe pode remover outro administrador");
        }

        desligar(teamId, userId);
    }

    public void leave(Long teamId) {
        if (access.role(teamId) == TeamRole.OWNER) {
            throw new ForbiddenException(
                    "O dono não pode sair da própria equipe. Transfira a posse ou exclua a equipe.");
        }
        desligar(teamId, currentUser.id());
    }

    private void desligar(Long teamId, Long userId) {
        cardRepo.deleteAssigneesOfUserInTeam(teamId, userId);
        boardMemberRepo.deleteByTeamIdAndUserId(teamId, userId);
        memberRepo.deleteByTeamIdAndUserId(teamId, userId);
    }

    private TeamMember findMemberOrThrow(Long teamId, Long userId) {
        return memberRepo.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Usuário %d não é membro da equipe %d".formatted(userId, teamId)));
    }
}
