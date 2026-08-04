package com.sergio.planix.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Perfil")
public class AvatarController {

    private final ProfileService service;

    public AvatarController(ProfileService service) { this.service = service; }

    @Operation(summary = "Baixar a foto de perfil de um usuário",
               description = """
                       É a URL que aparece como `avatarUrl` nas respostas. Qualquer conta
                       autenticada pode ver a foto de qualquer usuário — é o que permite mostrar
                       o rosto dos membros no quadro.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A imagem",
                         content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE,
                                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "404", description = "Usuário não existe ou não tem foto"),
            @ApiResponse(responseCode = "500", description = "O caminho existe, mas o arquivo sumiu do disco")
    })
    @GetMapping("/api/users/{id}/avatar")
    public ResponseEntity<Resource> avatar(
            @Parameter(description = "Id do usuário") @PathVariable Long id) {

        Resource resource = service.avatarOf(id);
        MediaType contentType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }
}
