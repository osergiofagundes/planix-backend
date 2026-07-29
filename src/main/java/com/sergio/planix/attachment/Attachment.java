package com.sergio.planix.attachment;

import com.sergio.planix.card.Card;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Metadados do anexo — o conteúdo do arquivo fica em disco. Não estende BaseEntity porque a
 * tabela só tem created_at: um anexo não é "atualizado", ele é trocado por outro.
 */
@Entity
@Table(name = "attachments")
@Getter
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Attachment() {}

    public Attachment(Card card, String originalFilename, String storedFilename,
                      String contentType, Long sizeBytes) {
        this.card = card;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
