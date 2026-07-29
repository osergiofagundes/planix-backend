package com.sergio.planix.checklist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

    List<ChecklistItem> findByCardIdOrderByPositionAsc(Long cardId);

    int countByCardId(Long cardId);
}
