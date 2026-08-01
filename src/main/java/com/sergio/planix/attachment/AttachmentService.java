package com.sergio.planix.attachment;

import com.sergio.planix.attachment.dto.AttachmentResponse;
import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.board.BoardAccess;
import com.sergio.planix.card.Card;
import com.sergio.planix.card.CardAccess;
import com.sergio.planix.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
public class AttachmentService {

    private final AttachmentRepository repo;
    private final FileStorageService storage;
    private final CardAccess cardAccess;
    private final BoardAccess boardAccess;
    private final CurrentUser currentUser;

    public AttachmentService(AttachmentRepository repo, FileStorageService storage,
                             CardAccess cardAccess, BoardAccess boardAccess, CurrentUser currentUser) {
        this.repo = repo;
        this.storage = storage;
        this.cardAccess = cardAccess;
        this.boardAccess = boardAccess;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Long cardId) {
        cardAccess.require(cardId);
        return repo.findByCardIdOrderByCreatedAtDesc(cardId).stream().map(AttachmentResponse::from).toList();
    }

    public AttachmentResponse upload(Long cardId, MultipartFile file) {
        Card card = cardAccess.require(cardId);
        String stored = storage.store(file);
        Attachment a = new Attachment(card, currentUser.reference(), file.getOriginalFilename(),
                stored, file.getContentType(), file.getSize());
        return AttachmentResponse.from(repo.save(a));
    }

    public void delete(Long id) {
        Attachment a = getEntity(id);
        repo.delete(a);
        storage.delete(a.getStoredFilename());
    }

    @Transactional(readOnly = true)
    public Attachment getEntity(Long id) {
        Attachment a = repo.findById(id).orElseThrow(() -> naoEncontrado(id));
        if (!boardAccess.isMember(a.getCard().getList().getBoard().getId())) {
            throw naoEncontrado(id);
        }
        return a;
    }

    private NotFoundException naoEncontrado(Long id) {
        return new NotFoundException("Anexo %d não encontrado".formatted(id));
    }
}
