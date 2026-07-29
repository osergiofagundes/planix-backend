package com.sergio.planix.attachment;

import com.sergio.planix.attachment.dto.AttachmentResponse;
import com.sergio.planix.card.Card;
import com.sergio.planix.card.CardRepository;
import com.sergio.planix.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
public class AttachmentService {

    private final AttachmentRepository repo;
    private final CardRepository cardRepo;
    private final FileStorageService storage;

    public AttachmentService(AttachmentRepository repo, CardRepository cardRepo, FileStorageService storage) {
        this.repo = repo;
        this.cardRepo = cardRepo;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Long cardId) {
        if (!cardRepo.existsById(cardId)) {
            throw new NotFoundException("Cartão %d não encontrado".formatted(cardId));
        }
        return repo.findByCardIdOrderByCreatedAtDesc(cardId).stream().map(AttachmentResponse::from).toList();
    }

    public AttachmentResponse upload(Long cardId, MultipartFile file) {
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Cartão %d não encontrado".formatted(cardId)));
        String stored = storage.store(file);
        Attachment a = new Attachment(card, file.getOriginalFilename(), stored,
                file.getContentType(), file.getSize());
        return AttachmentResponse.from(repo.save(a));
    }

    public void delete(Long id) {
        Attachment a = getEntity(id);
        repo.delete(a);
        storage.delete(a.getStoredFilename());   // apaga o arquivo do disco também
    }

    @Transactional(readOnly = true)
    public Attachment getEntity(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Anexo %d não encontrado".formatted(id)));
    }
}
