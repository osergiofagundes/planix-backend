package com.sergio.planix.card;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardChangeRepository extends JpaRepository<CardChange, Long> {

    List<CardChange> findByCardIdOrderByChangedAtDesc(Long cardId);
}
