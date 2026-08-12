package com.sergio.planix.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardMemberRepository extends JpaRepository<BoardMember, Long> {

    boolean existsByBoardIdAndUserId(Long boardId, Long userId);

    Optional<BoardMember> findByBoardIdAndUserId(Long boardId, Long userId);

    List<BoardMember> findByBoardIdOrderByCreatedAtAsc(Long boardId);

    void deleteByBoardIdAndUserId(Long boardId, Long userId);

    @Modifying
    @Query("""
            delete from BoardMember m
            where m.user.id = :userId
              and m.board.id in (select b.id from Board b where b.team.id = :teamId)
            """)
    int deleteByTeamIdAndUserId(@Param("teamId") Long teamId, @Param("userId") Long userId);
}
