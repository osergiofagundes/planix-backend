package com.sergio.planix.link;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardLinkRepository extends JpaRepository<CardLink, Long> {

    List<CardLink> findByCardIdOrderByCreatedAtDesc(Long cardId);
}
