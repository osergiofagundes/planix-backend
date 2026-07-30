package com.sergio.planix.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Segredos que o servidor gera e entrega uma única vez: refresh token, link de convite. O valor vai
 * para o cliente, o SHA-256 vai para o banco — vazou o banco, não vazou nenhum segredo usável.
 *
 * <p>Dois donos para o mesmo código é o momento de mover: se um dia o SHA-256 virar outra coisa,
 * troca-se num lugar só, com certeza de ter trocado em todos.
 */
public final class Tokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Tokens() {}

    /** 32 bytes de aleatoriedade criptográfica, em Base64 seguro para URL. */
    public static String random() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);   // 64 caracteres — o tamanho da coluna
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
