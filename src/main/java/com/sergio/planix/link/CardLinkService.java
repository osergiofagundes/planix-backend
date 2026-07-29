package com.sergio.planix.link;

import com.sergio.planix.card.Card;
import com.sergio.planix.card.CardRepository;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.link.dto.CardLinkRequest;
import com.sergio.planix.link.dto.CardLinkResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CardLinkService {

    private final CardLinkRepository repo;
    private final CardRepository cardRepo;

    public CardLinkService(CardLinkRepository repo, CardRepository cardRepo) {
        this.repo = repo;
        this.cardRepo = cardRepo;
    }

    @Transactional(readOnly = true)
    public List<CardLinkResponse> listByCard(Long cardId) {
        if (!cardRepo.existsById(cardId)) {
            throw new NotFoundException("Cartão %d não encontrado".formatted(cardId));
        }
        return repo.findByCardIdOrderByCreatedAtDesc(cardId).stream().map(CardLinkResponse::from).toList();
    }

    public CardLinkResponse create(Long cardId, CardLinkRequest req) {
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Cartão %d não encontrado".formatted(cardId)));
        return CardLinkResponse.from(repo.save(new CardLink(card, req.url(), req.title())));
    }

    public CardLinkResponse update(Long id, CardLinkRequest req) {
        CardLink link = findOrThrow(id);
        link.setUrl(req.url());
        link.setTitle(req.title());
        return CardLinkResponse.from(link);
    }

    public void delete(Long id) {
        repo.delete(findOrThrow(id));
    }

    private CardLink findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Link %d não encontrado".formatted(id)));
    }
}
