package com.sergio.planix.card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
