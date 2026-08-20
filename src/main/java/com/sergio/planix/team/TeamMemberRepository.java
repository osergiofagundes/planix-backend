package com.sergio.planix.team;

import com.sergio.planix.team.dto.TeamCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    List<TeamMember> findByTeamIdOrderByCreatedAtAsc(Long teamId);

    void deleteByTeamIdAndUserId(Long teamId, Long userId);

    @Query("""
            select m from TeamMember m
            join fetch m.team t
            join fetch t.owner
            where m.user.id = :userId
            order by t.name asc
            """)
    List<TeamMember> findMembershipsOf(@Param("userId") Long userId);

    @Query("""
            select new com.sergio.planix.team.dto.TeamCount(m.team.id, count(m))
            from TeamMember m
            where m.team.id in :teamIds
            group by m.team.id
            """)
    List<TeamCount> countMembersByTeam(@Param("teamIds") Collection<Long> teamIds);

    @Query("""
            select m.user.id from TeamMember m
            where m.team.id = :teamId
              and m.role in (com.sergio.planix.team.TeamRole.OWNER,
                             com.sergio.planix.team.TeamRole.ADMIN)
            """)
    List<Long> findManagerIds(@Param("teamId") Long teamId);
}
