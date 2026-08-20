package com.sergio.planix.notification;

import com.sergio.planix.board.BoardMemberRepository;
import com.sergio.planix.card.CardRepository;
import com.sergio.planix.team.TeamMemberRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class RecipientResolver {

    private final BoardMemberRepository boardMemberRepo;
    private final CardRepository cardRepo;
    private final TeamMemberRepository teamMemberRepo;

    public RecipientResolver(BoardMemberRepository boardMemberRepo, CardRepository cardRepo,
                             TeamMemberRepository teamMemberRepo) {
        this.boardMemberRepo = boardMemberRepo;
        this.cardRepo = cardRepo;
        this.teamMemberRepo = teamMemberRepo;
    }

    public List<Long> doQuadro(Long boardId, Long excluir) {
        return semOAtor(boardMemberRepo.findAudienceIds(boardId), excluir);
    }

    public List<Long> responsaveisDoCard(Long cardId, Long excluir) {
        return semOAtor(cardRepo.findAssigneeIds(cardId), excluir);
    }

    public List<Long> gestoresDaEquipe(Long teamId, Long excluir) {
        return semOAtor(teamMemberRepo.findManagerIds(teamId), excluir);
    }

    private List<Long> semOAtor(List<Long> ids, Long excluir) {
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(id -> !id.equals(excluir))
                .toList();
    }
}
