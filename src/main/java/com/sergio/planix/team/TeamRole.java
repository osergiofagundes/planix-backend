package com.sergio.planix.team;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        O que a pessoa pode fazer dentro da equipe.

        - `OWNER` — dona da equipe. Uma por equipe; muda por transferência de posse.
        - `ADMIN` — administra a equipe e manda em qualquer quadro dela.
        - `MEMBER` — usa os quadros a que tem acesso.""")
public enum TeamRole {

    OWNER,
    ADMIN,
    MEMBER;

    public boolean isAdmin() {
        return this == OWNER || this == ADMIN;
    }
}
