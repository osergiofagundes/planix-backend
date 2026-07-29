package com.sergio.planix.attachment;

import com.sergio.planix.attachment.dto.AttachmentResponse;
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
public class AttachmentController {

    private final AttachmentService service;
    private final FileStorageService storage;

    public AttachmentController(AttachmentService service, FileStorageService storage) {
        this.service = service;
        this.storage = storage;
    }

    @PostMapping(value = "/cards/{cardId}/attachments",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse upload(@PathVariable Long cardId,
                                     @RequestParam("file") MultipartFile file) {
        return service.upload(cardId, file);
    }

    @GetMapping("/cards/{cardId}/attachments")
    public List<AttachmentResponse> list(@PathVariable Long cardId) {
        return service.list(cardId);
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
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

    @DeleteMapping("/attachments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
