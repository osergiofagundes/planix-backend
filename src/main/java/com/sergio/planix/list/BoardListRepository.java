package com.sergio.planix.list;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BoardListRepository extends JpaRepository<BoardList, Long> {

    List<BoardList> findByBoardIdOrderByPositionAsc(Long boardId);

    boolean existsByBoardId(Long boardId);

    int countByBoardId(Long boardId);
}