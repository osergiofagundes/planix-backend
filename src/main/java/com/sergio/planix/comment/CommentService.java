package com.sergio.planix.comment;

import com.sergio.planix.card.Card;
import com.sergio.planix.card.CardRepository;
import com.sergio.planix.comment.dto.CommentRequest;
import com.sergio.planix.comment.dto.CommentResponse;
import com.sergio.planix.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CommentService {

    private final CommentRepository repo;
    private final CardRepository cardRepo;

    public CommentService(CommentRepository repo, CardRepository cardRepo) {
        this.repo = repo;
        this.cardRepo = cardRepo;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listByCard(Long cardId) {
        if (!cardRepo.existsById(cardId)) {
            throw new NotFoundException("Cartão %d não encontrado".formatted(cardId));
        }
        return repo.findByCardIdOrderByCreatedAtDesc(cardId).stream().map(CommentResponse::from).toList();
    }

    public CommentResponse create(Long cardId, CommentRequest req) {
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Cartão %d não encontrado".formatted(cardId)));
        return CommentResponse.from(repo.save(new Comment(card, req.text())));
    }

    public CommentResponse update(Long id, CommentRequest req) {
        Comment comment = findOrThrow(id);
        comment.setText(req.text());
        return CommentResponse.from(comment);
    }

    public void delete(Long id) {
        repo.delete(findOrThrow(id));
    }

    private Comment findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Comentário %d não encontrado".formatted(id)));
    }
}
