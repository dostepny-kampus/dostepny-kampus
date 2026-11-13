/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity;

import java.util.List;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Profil zalogowanej osoby, schemat CurrentUser w OpenAPI.
 *
 * <p>Wymagania: US14.1, BR-1</p>
 *
 * @param id Identyfikator konta w Usłudze logowania
 * @param displayName Imię i nazwisko albo nazwa konta
 * @param email Adres e-mail konta
 * @param roles Role osoby w kolejności z kontraktu
 * @since 0.1.0
 */
record CurrentUserResponse(String id, String displayName, String email, List<String> roles) {

    /**
     * Rola, którą ma każde konto.
     *
     * <p>Wymagania: BR-1, US14.3</p>
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
            CurrentUserResponse.claim(
                account, "name",
                CurrentUserResponse.claim(account, "preferred_username", account.getName())
            ),
            CurrentUserResponse.claim(account, "email", ""),
            CurrentUserResponse.ROLES.stream()
                .filter(role -> CurrentUserResponse.granted(account, role)).toList()
        );
    }

    private static String claim(
        final JwtAuthenticationToken account, final String name, final String fallback
    ) {
        String text = fallback;
        if (account.getTokenAttributes().get(name) instanceof String found) {
            text = found;
        }
        return text;
    }

    private static boolean granted(final JwtAuthenticationToken account, final String role) {
        return CurrentUserResponse.VISITOR.equals(role) || account.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_%s".formatted(role).equals(authority.getAuthority()));
    }
}
