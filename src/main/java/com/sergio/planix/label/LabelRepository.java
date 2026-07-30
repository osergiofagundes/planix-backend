package com.sergio.planix.label;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabelRepository extends JpaRepository<Label, Long> {

    List<Label> findByBoardIdOrderByNameAsc(Long boardId);

    boolean existsByBoardIdAndName(Long boardId, String name);
}
