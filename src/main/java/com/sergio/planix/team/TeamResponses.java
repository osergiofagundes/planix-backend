package com.sergio.planix.team;

import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.board.BoardRepository;
import com.sergio.planix.team.dto.TeamCount;
import com.sergio.planix.team.dto.TeamResponse;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TeamResponses {

    private final TeamMemberRepository memberRepo;
    private final BoardRepository boardRepo;
    private final CurrentUser currentUser;

    public TeamResponses(TeamMemberRepository memberRepo, BoardRepository boardRepo,
                         CurrentUser currentUser) {
        this.memberRepo = memberRepo;
        this.boardRepo = boardRepo;
        this.currentUser = currentUser;
    }

    public TeamResponse of(Team team, TeamRole myRole) {
        List<Long> ids = List.of(team.getId());
        return TeamResponse.of(team, myRole,
                count(memberRepo.countMembersByTeam(ids), team.getId()),
                count(boardRepo.countAccessibleByTeam(ids, currentUser.id()), team.getId()));
    }

    public List<TeamResponse> ofAll(List<TeamMember> memberships) {
        if (memberships.isEmpty()) {
            return List.of();
        }

        List<Long> ids = memberships.stream().map(m -> m.getTeam().getId()).toList();
        Map<Long, Long> membros = porEquipe(memberRepo.countMembersByTeam(ids));
        Map<Long, Long> quadros = porEquipe(boardRepo.countAccessibleByTeam(ids, currentUser.id()));

        return memberships.stream()
                .map(m -> TeamResponse.of(m.getTeam(), m.getRole(),
                        membros.getOrDefault(m.getTeam().getId(), 0L),
                        quadros.getOrDefault(m.getTeam().getId(), 0L)))
                .toList();
    }

    private static Map<Long, Long> porEquipe(Collection<TeamCount> counts) {
        return counts.stream().collect(
                Collectors.toMap(TeamCount::teamId, TeamCount::count, Long::sum));
    }

    private static long count(Collection<TeamCount> counts, Long teamId) {
        return counts.stream()
                .filter(c -> teamId.equals(c.teamId()))
                .map(TeamCount::count)
                .findFirst()
                .orElse(0L);
    }
}
