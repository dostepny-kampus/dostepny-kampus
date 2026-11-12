/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity;

import java.util.List;
import java.util.Objects;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Profil zalogowanej osoby, schemat CurrentUser w OpenAPI (US14.1, BR-1).
 *
 * @param id Identyfikator konta w Usłudze logowania
 * @param displayName Imię i nazwisko albo nazwa konta
 * @param email Adres e-mail konta
 * @param roles Role osoby w kolejności z kontraktu
 * @since 0.1.0
 */
record CurrentUserResponse(String id, String displayName, String email, List<String> roles) {

    /**
     * Rola, którą ma każde konto (BR-1, US14.3).
     */
    private static final String VISITOR = "VISITOR";

    /**
     * Role z kontraktu w kolejności schematu Role.
     */
    private static final List<String> ROLES = List.of(
        CurrentUserResponse.VISITOR, "COLLECTOR", "VERIFIER", "COORDINATOR"
    );

    /**
     * Czyta profil z tokenu: identyfikator z {@code sub}, nazwę z {@code name},
     * a bez niej z {@code preferred_username}, adres z {@code email}. Role to
     * role konta znane kontraktowi; każde konto ma rolę Odwiedzającego.
     *
     * @param account Zalogowane konto z tokenem
     */
    CurrentUserResponse(final JwtAuthenticationToken account) {
        this(
            account.getName(),
            CurrentUserResponse.name(account.getToken(), account.getName()),
            Objects.requireNonNullElse(account.getToken().getClaimAsString("email"), ""),
            CurrentUserResponse.ROLES.stream()
                .filter(role -> CurrentUserResponse.granted(account, role)).toList()
        );
    }

    private static String name(final Jwt token, final String fallback) {
        return Objects.requireNonNullElse(
            token.getClaimAsString("name"),
            Objects.requireNonNullElse(token.getClaimAsString("preferred_username"), fallback)
        );
    }

    private static boolean granted(final JwtAuthenticationToken account, final String role) {
        return CurrentUserResponse.VISITOR.equals(role) || account.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_%s".formatted(role).equals(authority.getAuthority()));
    }
}
