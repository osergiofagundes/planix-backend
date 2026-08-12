package com.sergio.planix.team;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.User;
import com.sergio.planix.auth.UserRepository;
import com.sergio.planix.board.BoardRepository;
import com.sergio.planix.common.exception.NotFoundException;
import com.sergio.planix.common.exception.TeamNotEmptyException;
import com.sergio.planix.team.dto.TeamRequest;
import com.sergio.planix.team.dto.TeamResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TeamService {

    private final TeamRepository repo;
    private final TeamMemberRepository memberRepo;
    private final BoardRepository boardRepo;
    private final UserRepository userRepo;
    private final TeamAccess access;
    private final TeamResponses responses;
    private final CurrentUser currentUser;

    public TeamService(TeamRepository repo, TeamMemberRepository memberRepo,
                       BoardRepository boardRepo, UserRepository userRepo, TeamAccess access,
                       TeamResponses responses, CurrentUser currentUser) {
        this.repo = repo;
        this.memberRepo = memberRepo;
        this.boardRepo = boardRepo;
        this.userRepo = userRepo;
        this.access = access;
        this.responses = responses;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> list() {
        return responses.ofAll(memberRepo.findMembershipsOf(currentUser.id()));
    }

    @Transactional(readOnly = true)
    public TeamResponse get(Long id) {
        return responses.of(findOrThrow(id), access.role(id));
    }

    public TeamResponse create(TeamRequest req) {
        User me = currentUser.reference();
        Team saved = repo.save(new Team(me, req.name(), req.description(), req.icon()));
        memberRepo.save(new TeamMember(saved, me, TeamRole.OWNER));
        return responses.of(saved, TeamRole.OWNER);
    }

    public TeamResponse update(Long id, TeamRequest req) {
        access.requireAdmin(id);
        Team team = findOrThrow(id);
        team.setName(req.name());
        team.setDescription(req.description());
        team.setIcon(req.icon());
        return responses.of(team, access.role(id));
    }

    public TeamResponse transferOwnership(Long id, Long newOwnerId) {
        access.requireOwner(id);
        Team team = findOrThrow(id);

        TeamMember novo = memberRepo.findByTeamIdAndUserId(id, newOwnerId)
                .orElseThrow(() -> new NotFoundException(
                        "Usuário %d não é membro da equipe %d".formatted(newOwnerId, id)));

        memberRepo.findByTeamIdAndUserId(id, currentUser.id())
                .ifPresent(antigo -> antigo.setRole(TeamRole.ADMIN));
        novo.setRole(TeamRole.OWNER);
        team.setOwner(userRepo.getReferenceById(newOwnerId));

        return responses.of(team, TeamRole.ADMIN);
    }

    public void delete(Long id, String confirmationName) {
        access.requireOwner(id);
        Team team = findOrThrow(id);

        if (boardRepo.existsByTeamId(id) && !team.getName().equals(confirmationName)) {
            throw new TeamNotEmptyException(
                    "A equipe possui quadros. Para excluir, confirme digitando o nome exato da equipe.");
        }
        repo.delete(team);
    }

    private Team findOrThrow(Long id) {
        access.requireMember(id);
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipe %d não encontrada".formatted(id)));
    }
}
