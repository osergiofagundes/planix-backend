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

    /*
     * Espelha a regra do BoardRepository.hasAccess: dono, membros explícitos e — quando o
     * quadro é aberto à equipe, ou a pessoa administra — os membros da equipe. Se a regra
     * de acesso mudar lá, muda aqui: são as duas faces do mesmo critério.
     *
     * Nativa e em union porque o JPQL equivalente partiria de User e varreria a tabela
     * inteira para só depois filtrar.
     */
    @Query(value = """
            select b.owner_id from boards b where b.id = :boardId
            union
            select m.user_id from board_members m where m.board_id = :boardId
            union
            select tm.user_id
              from team_members tm
              join boards b on b.team_id = tm.team_id
             where b.id = :boardId
               and (b.visibility = 'TEAM' or tm.role in ('OWNER', 'ADMIN'))
            """, nativeQuery = true)
    List<Long> findAudienceIds(@Param("boardId") Long boardId);
}
