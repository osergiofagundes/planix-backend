package com.sergio.planix.attachment;

import com.sergio.planix.attachment.dto.AttachmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Anexos")
public class AttachmentController {

    private final AttachmentService service;
    private final FileStorageService storage;

    public AttachmentController(AttachmentService service, FileStorageService storage) {
        this.service = service;
        this.storage = storage;
    }

    @Operation(summary = "Enviar um anexo para um cartão",
               description = """
                       Envio `multipart/form-data` com um campo `file`. O arquivo vai para o disco;
                       o banco guarda só os metadados. Limite de **10 MB** por arquivo.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Anexo enviado"),
            @ApiResponse(responseCode = "404", description = "Cartão não encontrado"),
            @ApiResponse(responseCode = "413", description = "Arquivo maior que o limite de 10 MB"),
            @ApiResponse(responseCode = "500", description = "Falha ao gravar o arquivo em disco")
    })
    @PostMapping(value = "/cards/{cardId}/attachments",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse upload(@Parameter(description = "Id do cartão") @PathVariable Long cardId,
                                     @Parameter(description = "O arquivo a enviar",
                                                content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                                                                   schema = @Schema(type = "string", format = "binary")))
                                     @RequestParam("file") MultipartFile file) {
        return service.upload(cardId, file);
    }

    @Operation(summary = "Listar os anexos de um cartão",
               description = "Só os metadados (nome original, tipo, tamanho). O conteúdo vem pelo download.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Anexos do cartão"),
            @ApiResponse(responseCode = "404", description = "Cartão não encontrado")
    })
    @GetMapping("/cards/{cardId}/attachments")
    public List<AttachmentResponse> list(@Parameter(description = "Id do cartão") @PathVariable Long cardId) {
        return service.list(cardId);
    }

    @Operation(summary = "Baixar um anexo",
               description = "Devolve o arquivo com o `Content-Type` registrado no envio e um "
                           + "`Content-Disposition: attachment` com o nome original.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "O arquivo",
                         content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "404", description = "Anexo não encontrado"),
            @ApiResponse(responseCode = "500", description = "Metadados existem, mas o arquivo sumiu do disco")
    })
    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<Resource> download(@Parameter(description = "Id do anexo") @PathVariable Long id) {
        Attachment a = service.getEntity(id);
        Resource resource = storage.loadAsResource(a.getStoredFilename());
        String contentType = a.getContentType() != null
                ? a.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + a.getOriginalFilename() + "\"")
                .body(resource);
    }

    @Operation(summary = "Excluir um anexo",
               description = "Apaga o registro e o arquivo em disco.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Anexo excluído", content = @Content),
            @ApiResponse(responseCode = "404", description = "Anexo não encontrado")
    })
    @DeleteMapping("/attachments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Id do anexo") @PathVariable Long id) {
        service.delete(id);
    }
}
