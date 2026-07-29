package com.sergio.planix.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    @Query("select b from Board b where b.owner.id = :userId")
    List<Board> findAccessibleBy(@Param("userId") Long userId);
}
