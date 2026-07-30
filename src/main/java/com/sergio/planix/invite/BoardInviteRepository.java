package com.sergio.planix.invite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardInviteRepository extends JpaRepository<BoardInvite, Long> {

    Optional<BoardInvite> findByTokenHash(String tokenHash);

    List<BoardInvite> findByBoardIdOrderByCreatedAtDesc(Long boardId);

    @Modifying
    @Query("update BoardInvite i set i.uses = i.uses + 1 where i.id = :id and i.uses < i.maxUses")
    int consume(@Param("id") Long id);
}
