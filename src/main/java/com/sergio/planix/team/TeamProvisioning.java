package com.sergio.planix.team;

import com.sergio.planix.auth.User;
import org.springframework.stereotype.Component;

@Component
public class TeamProvisioning {

    private static final int NOME_MAX = 150;

    private final TeamRepository teamRepo;
    private final TeamMemberRepository memberRepo;

    public TeamProvisioning(TeamRepository teamRepo, TeamMemberRepository memberRepo) {
        this.teamRepo = teamRepo;
        this.memberRepo = memberRepo;
    }

    public Team createFirstTeamFor(User user) {
        String nome = "Equipe de " + user.getName();
        Team team = teamRepo.save(new Team(user,
                nome.length() > NOME_MAX ? nome.substring(0, NOME_MAX) : nome, null, null));
        memberRepo.save(new TeamMember(team, user, TeamRole.OWNER));
        return team;
    }
}
