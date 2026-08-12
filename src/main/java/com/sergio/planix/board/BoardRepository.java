package com.sergio.planix.board;

import com.sergio.planix.team.dto.TeamCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByTeamId(Long teamId);

    @Query("""
            select count(b) > 0 from Board b
            where b.id = :boardId
              and (b.owner.id = :userId
                   or exists (select 1 from BoardMember m
                               where m.board = b and m.user.id = :userId)
                   or exists (select 1 from TeamMember tm
                               where tm.team = b.team and tm.user.id = :userId
                                 and (b.visibility = com.sergio.planix.board.BoardVisibility.TEAM
                                      or tm.role in (com.sergio.planix.team.TeamRole.OWNER,
                                                     com.sergio.planix.team.TeamRole.ADMIN))))
            """)
    boolean hasAccess(@Param("boardId") Long boardId, @Param("userId") Long userId);

    @Query("""
            select count(b) > 0 from Board b
            where b.id = :boardId
              and (b.owner.id = :userId
                   or exists (select 1 from TeamMember tm
                               where tm.team = b.team and tm.user.id = :userId
                                 and tm.role in (com.sergio.planix.team.TeamRole.OWNER,
                                                 com.sergio.planix.team.TeamRole.ADMIN)))
            """)
    boolean canManage(@Param("boardId") Long boardId, @Param("userId") Long userId);

    @Query("""
            select b from Board b
            where (b.owner.id = :userId
                   or exists (select 1 from BoardMember m
                               where m.board = b and m.user.id = :userId)
                   or exists (select 1 from TeamMember tm
                               where tm.team = b.team and tm.user.id = :userId
                                 and (b.visibility = com.sergio.planix.board.BoardVisibility.TEAM
                                      or tm.role in (com.sergio.planix.team.TeamRole.OWNER,
                                                     com.sergio.planix.team.TeamRole.ADMIN))))
            order by b.name asc
            """)
    List<Board> findAccessibleBy(@Param("userId") Long userId);

    @Query("""
            select new com.sergio.planix.team.dto.TeamCount(b.team.id, count(b))
            from Board b
            where b.team.id in :teamIds
              and (b.owner.id = :userId
                   or exists (select 1 from BoardMember m
                               where m.board = b and m.user.id = :userId)
                   or exists (select 1 from TeamMember tm
                               where tm.team = b.team and tm.user.id = :userId
                                 and (b.visibility = com.sergio.planix.board.BoardVisibility.TEAM
                                      or tm.role in (com.sergio.planix.team.TeamRole.OWNER,
                                                     com.sergio.planix.team.TeamRole.ADMIN))))
            group by b.team.id
            """)
    List<TeamCount> countAccessibleByTeam(@Param("teamIds") Collection<Long> teamIds,
                                          @Param("userId") Long userId);

    @Query("""
            select b from Board b
            where b.team.id = :teamId
              and (b.owner.id = :userId
                   or exists (select 1 from BoardMember m
                               where m.board = b and m.user.id = :userId)
                   or exists (select 1 from TeamMember tm
                               where tm.team = b.team and tm.user.id = :userId
                                 and (b.visibility = com.sergio.planix.board.BoardVisibility.TEAM
                                      or tm.role in (com.sergio.planix.team.TeamRole.OWNER,
                                                     com.sergio.planix.team.TeamRole.ADMIN))))
            order by b.name asc
            """)
    List<Board> findAccessibleIn(@Param("teamId") Long teamId, @Param("userId") Long userId);
}
