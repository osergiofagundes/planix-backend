package com.sergio.planix.attachment.dto;

import com.sergio.planix.attachment.Attachment;
import com.sergio.planix.auth.dto.UserSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Metadados de um anexo. O conteúdo do arquivo vem por "
                    + "`GET /api/attachments/{id}/download`.")
public record AttachmentResponse(
        @Schema(description = "Id do anexo", example = "500") Long id,
        @Schema(description = "Cartão a que o anexo pertence", example = "100") Long cardId,
        @Schema(description = "Nome do arquivo como veio da máquina de quem enviou",
                example = "orcamento-dominio.pdf")
        String originalFilename,
        @Schema(description = "Nome com que o arquivo foi gravado em disco. Gerado pela API para "
                            + "evitar colisão entre arquivos de mesmo nome.",
                example = "a3f1c9e2-77b4-4d10-9e6a-1c8d5f0b2e34.pdf")
        String storedFilename,
        @Schema(description = "Tipo MIME informado no envio", example = "application/pdf")
        String contentType,
        @Schema(description = "Tamanho do arquivo em bytes", example = "184320") Long sizeBytes,
        @Schema(description = "Quem enviou o arquivo") UserSummary author,
        @Schema(description = "Quando foi enviado", example = "2026-07-15T09:12:44.518-03:00")
        OffsetDateTime createdAt
) {
    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getCard().getId(), a.getOriginalFilename(),
                a.getStoredFilename(), a.getContentType(), a.getSizeBytes(),
                UserSummary.from(a.getAuthor()), a.getCreatedAt());
    }
}
