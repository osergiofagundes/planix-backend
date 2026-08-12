package com.sergio.planix.invite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamInviteRepository extends JpaRepository<TeamInvite, Long> {

    Optional<TeamInvite> findByTokenHash(String tokenHash);

    List<TeamInvite> findByTeamIdOrderByCreatedAtDesc(Long teamId);

    @Modifying
    @Query("update TeamInvite i set i.uses = i.uses + 1 where i.id = :id and i.uses < i.maxUses")
    int consume(@Param("id") Long id);
}
