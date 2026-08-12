package com.sergio.planix.auth;

import com.sergio.planix.auth.dto.AuthResponse;
import com.sergio.planix.auth.dto.EmailChangeRequest;
import com.sergio.planix.auth.dto.PasswordChangeRequest;
import com.sergio.planix.common.exception.EmailAlreadyUsedException;
import com.sergio.planix.common.exception.InvalidFieldException;
import com.sergio.planix.common.exception.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final AuthService authService;
    private final CurrentUser currentUser;

    public AccountService(UserRepository userRepo, PasswordEncoder encoder,
                          AuthService authService, CurrentUser currentUser) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.authService = authService;
        this.currentUser = currentUser;
    }

    public AuthResponse changeEmail(EmailChangeRequest req) {
        User user = usuarioAtual();
        confereSenhaAtual(user, req.currentPassword());

        String novo = Emails.normalize(req.newEmail());
        if (!novo.equals(user.getEmail()) && userRepo.existsByEmail(novo)) {
            throw new EmailAlreadyUsedException("Já existe uma conta com o e-mail " + novo);
        }

        user.setEmail(novo);
        return authService.rotateSessions(user);
    }

    public AuthResponse changePassword(PasswordChangeRequest req) {
        User user = usuarioAtual();
        confereSenhaAtual(user, req.currentPassword());

        if (encoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw new InvalidFieldException("newPassword",
                    "a nova senha precisa ser diferente da atual");
        }

        user.setPasswordHash(encoder.encode(req.newPassword()));
        return authService.rotateSessions(user);
    }

    private void confereSenhaAtual(User user, String senha) {
        if (!encoder.matches(senha, user.getPasswordHash())) {
            throw new InvalidFieldException("currentPassword", "senha incorreta");
        }
    }

    private User usuarioAtual() {
        return userRepo.findById(currentUser.id())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }
}
