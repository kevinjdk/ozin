package com.ozin.application.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Genera tokens aleatorios y calcula su hash.
 *
 * <p>Se guarda solo el SHA-256: una filtración de la base de datos no permite usar los tokens.
 * Un hash rápido es suficiente aquí, porque el token tiene 256 bits de azar y no se puede adivinar.
 */
final class TokenSupport {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenSupport() {
    }

    static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }
}
