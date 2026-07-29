package com.sergio.planix.attachment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByCardIdOrderByCreatedAtDesc(Long cardId);
}
