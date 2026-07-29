package com.sergio.planix.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final UserRepository userRepo;

    public CurrentUser(UserRepository userRepo) { this.userRepo = userRepo; }

    public Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto");
        }
        return userId;
    }

    public User reference() {
        return userRepo.getReferenceById(id());
    }
}
