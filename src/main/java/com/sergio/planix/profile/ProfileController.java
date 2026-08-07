package com.sergio.planix.profile;

import com.sergio.planix.profile.dto.ProfileRequest;
import com.sergio.planix.profile.dto.ProfileResponse;
import com.sergio.planix.profile.dto.SocialLinkRequest;
import com.sergio.planix.profile.dto.SocialLinkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Perfil")
public class ProfileController {

    private final ProfileService service;
    private final SocialLinkService socialLinks;

    public ProfileController(ProfileService service, SocialLinkService socialLinks) {
        this.service = service;
        this.socialLinks = socialLinks;
    }

    @Operation(summary = "Ver meu perfil",
               description = "Tudo o que a conta autenticada tem preenchido, incluindo as redes sociais.")
    @ApiResponse(responseCode = "200", description = "Perfil da conta autenticada")
    @GetMapping("/profile")
    public ProfileResponse get() { return service.get(); }

    @Operation(summary = "Atualizar meu perfil",
               description = """
                       Substitui o perfil inteiro. Campo que vier `null` — ou que você simplesmente
                       omitir — é **apagado**: é assim que se remove uma informação.

                       Só o `name` é obrigatório. A foto tem endpoint próprio, e as credenciais
                       também: `PUT /api/me/email` e `PUT /api/me/password`.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (veja `fieldErrors`)")
    })
    @PutMapping("/profile")
    public ProfileResponse update(@Valid @RequestBody ProfileRequest req) {
        return service.update(req);
    }

    @Operation(summary = "Enviar minha foto de perfil",
               description = """
                       Envio `multipart/form-data` com um campo `file`. Aceita **JPEG, PNG ou WebP**
                       de até **2 MB**. O arquivo vai para `uploads/profile_pictures`; o banco guarda
                       só o caminho. Se já havia uma foto, a antiga é apagada do disco.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foto trocada; o perfil volta com o `avatarUrl`"),
            @ApiResponse(responseCode = "413", description = "Imagem maior que 2 MB"),
            @ApiResponse(responseCode = "415", description = "O arquivo não é JPEG, PNG nem WebP"),
            @ApiResponse(responseCode = "500", description = "Falha ao gravar o arquivo em disco")
    })
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse uploadAvatar(
            @Parameter(description = "A imagem a enviar",
                       content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                                          schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file) {
        return service.uploadAvatar(file);
    }

    @Operation(summary = "Remover minha foto de perfil",
               description = "Apaga o arquivo do disco e zera o `avatarUrl`. Sem foto, não faz nada.")
    @ApiResponse(responseCode = "204", description = "Foto removida", content = @Content)
    @DeleteMapping("/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvatar() { service.deleteAvatar(); }

    @Operation(summary = "Listar minhas redes sociais")
    @ApiResponse(responseCode = "200", description = "Redes cadastradas (vazio se não há nenhuma)")
    @GetMapping("/social-links")
    public List<SocialLinkResponse> listSocialLinks() { return socialLinks.list(); }

    @Operation(summary = "Cadastrar ou trocar a URL de uma rede",
               description = "É um PUT porque cada rede tem no máximo uma URL: mandar de novo "
                           + "na mesma rede **substitui**, não duplica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rede cadastrada ou atualizada"),
            @ApiResponse(responseCode = "400", description = "URL inválida ou rede desconhecida")
    })
    @PutMapping("/social-links/{platform}")
    public SocialLinkResponse upsertSocialLink(
            @Parameter(description = "A rede social") @PathVariable SocialPlatform platform,
            @Valid @RequestBody SocialLinkRequest req) {
        return socialLinks.upsert(platform, req.url());
    }

    @Operation(summary = "Remover uma rede social",
               description = "Idempotente: remover uma rede que não estava lá também devolve 204.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rede removida", content = @Content),
            @ApiResponse(responseCode = "400", description = "Rede desconhecida")
    })
    @DeleteMapping("/social-links/{platform}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSocialLink(
            @Parameter(description = "A rede social") @PathVariable SocialPlatform platform) {
        socialLinks.delete(platform);
    }
}
