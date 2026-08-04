package com.sergio.planix.profile;

import com.sergio.planix.auth.Address;
import com.sergio.planix.auth.CurrentUser;
import com.sergio.planix.auth.User;
import com.sergio.planix.auth.UserRepository;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.common.UnsupportedFileTypeException;
import com.sergio.planix.profile.dto.AddressRequest;
import com.sergio.planix.profile.dto.ProfileRequest;
import com.sergio.planix.profile.dto.ProfileResponse;
import com.sergio.planix.storage.FileStorageService;
import com.sergio.planix.storage.StorageFolder;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class ProfileService {

    /** Bem abaixo do limite global de 10 MB: foto de perfil não precisa de mais que isso. */
    static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;

    private static final Set<String> TIPOS_DE_IMAGEM =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final UserRepository userRepo;
    private final SocialLinkRepository socialRepo;
    private final FileStorageService storage;
    private final CurrentUser currentUser;

    public ProfileService(UserRepository userRepo, SocialLinkRepository socialRepo,
                          FileStorageService storage, CurrentUser currentUser) {
        this.userRepo = userRepo;
        this.socialRepo = socialRepo;
        this.storage = storage;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public ProfileResponse get() {
        return toResponse(usuarioAtual());
    }

    /**
     * PUT de verdade: o que não vier no corpo é apagado. É assim que o usuário "remove" uma
     * informação, sem precisar de um endpoint por campo.
     */
    public ProfileResponse update(ProfileRequest req) {
        User user = usuarioAtual();
        user.setName(req.name().trim());
        user.setBirthDate(req.birthDate());
        user.setPhone(vazioViraNulo(req.phone()));
        user.setBio(vazioViraNulo(req.bio()));
        user.setAddress(toAddress(req.address()));
        return toResponse(user);
    }

    public ProfileResponse uploadAvatar(MultipartFile file) {
        if (file.getContentType() == null || !TIPOS_DE_IMAGEM.contains(file.getContentType())) {
            throw new UnsupportedFileTypeException(
                    "A foto de perfil precisa ser JPEG, PNG ou WebP");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new MaxUploadSizeExceededException(MAX_AVATAR_BYTES);
        }

        User user = usuarioAtual();
        String anterior = user.getAvatarPath();

        user.setAvatarPath(storage.store(file, StorageFolder.PROFILE_PICTURES));
        if (anterior != null) {
            storage.delete(anterior);
        }
        return toResponse(user);
    }

    public void deleteAvatar() {
        User user = usuarioAtual();
        String atual = user.getAvatarPath();
        if (atual == null) return;

        user.setAvatarPath(null);
        storage.delete(atual);
    }

    /** A foto de qualquer usuário — é o que faz o avatar aparecer ao lado do nome nos quadros. */
    @Transactional(readOnly = true)
    public Resource avatarOf(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário %d não encontrado".formatted(userId)));
        if (user.getAvatarPath() == null) {
            throw new NotFoundException("Usuário %d não tem foto de perfil".formatted(userId));
        }
        return storage.loadAsResource(user.getAvatarPath());
    }

    private User usuarioAtual() {
        return userRepo.findById(currentUser.id())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    private ProfileResponse toResponse(User user) {
        return ProfileResponse.from(user, socialRepo.findByUserIdOrderByPlatformAsc(user.getId()));
    }

    private static Address toAddress(AddressRequest req) {
        if (req == null) return null;

        Address address = new Address(
                vazioViraNulo(req.street()), vazioViraNulo(req.number()),
                vazioViraNulo(req.complement()), vazioViraNulo(req.city()),
                maiusculo(req.state()), vazioViraNulo(req.zipCode()));

        boolean tudoVazio = address.getStreet() == null && address.getNumber() == null
                && address.getComplement() == null && address.getCity() == null
                && address.getState() == null && address.getZipCode() == null;

        return tudoVazio ? null : address;
    }

    /** String em branco e ausência são a mesma coisa aqui: o banco guarda só {@code null}. */
    private static String vazioViraNulo(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String maiusculo(String value) {
        String normalized = vazioViraNulo(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
