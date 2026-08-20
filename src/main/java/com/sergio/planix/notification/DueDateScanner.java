package com.sergio.planix.notification;

import com.sergio.planix.card.CardRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(name = "planix.due-scan.enabled", havingValue = "true", matchIfMissing = true)
public class DueDateScanner {

    private final CardRepository cardRepo;
    private final NotificationPublisher publisher;
    private final int janelaEmHoras;

    public DueDateScanner(CardRepository cardRepo, NotificationPublisher publisher,
                          @Value("${planix.due-scan.window-hours:24}") int janelaEmHoras) {
        this.cardRepo = cardRepo;
        this.publisher = publisher;
        this.janelaEmHoras = janelaEmHoras;
    }

    @Scheduled(cron = "${planix.due-scan.cron:0 5 * * * *}")
    @Transactional
    public int varrer() {
        OffsetDateTime agora = OffsetDateTime.now();

        List<com.sergio.planix.card.Card> proximos =
                cardRepo.findComPrazoProximo(agora, agora.plusHours(janelaEmHoras));
        proximos.forEach(publisher::cardDueSoon);

        List<com.sergio.planix.card.Card> vencidos = cardRepo.findVencidos(agora);
        vencidos.forEach(publisher::cardDueOverdue);

        return proximos.size() + vencidos.size();
    }
}
