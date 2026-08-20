package com.sergio.planix.card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByListIdOrderByPositionAsc(Long listId);

    int countByListId(Long listId);

    @Modifying
    @Query(value = """
            delete from card_assignees ca
            using cards c, board_lists l
            where ca.card_id = c.id
              and c.list_id  = l.id
              and l.board_id = :boardId
              and ca.user_id = :userId
            """, nativeQuery = true)
    int deleteAssigneesOfUserInBoard(@Param("boardId") Long boardId, @Param("userId") Long userId);

    @Modifying
    @Query(value = """
            delete from card_assignees ca
            using cards c, board_lists l, boards b
            where ca.card_id = c.id
              and c.list_id  = l.id
              and l.board_id = b.id
              and b.team_id  = :teamId
              and ca.user_id = :userId
            """, nativeQuery = true)
    int deleteAssigneesOfUserInTeam(@Param("teamId") Long teamId, @Param("userId") Long userId);

    @Query("select u.id from Card c join c.assignees u where c.id = :cardId")
    List<Long> findAssigneeIds(@Param("cardId") Long cardId);

    @Query("""
            select c from Card c
            where c.completed = false and c.dueDate is not null
              and c.dueDate > :agora and c.dueDate <= :limite
            """)
    List<Card> findComPrazoProximo(@Param("agora") OffsetDateTime agora,
                                   @Param("limite") OffsetDateTime limite);

    @Query("""
            select c from Card c
            where c.completed = false and c.dueDate is not null and c.dueDate <= :agora
            """)
    List<Card> findVencidos(@Param("agora") OffsetDateTime agora);
}
