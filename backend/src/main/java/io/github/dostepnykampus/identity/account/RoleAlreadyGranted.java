/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.account;

import io.github.dostepnykampus.shared.error.ConflictingState;
import java.net.URI;
import java.util.Map;

/**
 * Konto z adresem z zaproszenia ma już zapraszaną rolę; nic się nie zmieniło. Odpowiedź 409
 * ma rodzaj {@link #TYPE} i rolę we właściwości {@code role}.
 *
 * <p>Wymagania: US12.2</p>
 *
 * @since 0.1.0
 */
public final class RoleAlreadyGranted extends ConflictingState {

    /** Rodzaj błędu w polu {@code type} odpowiedzi, jak w kontrakcie API. */
    public static final URI TYPE = URI.create("urn:dostepny-kampus:rola-juz-nadana");

    /** Identyfikator postaci serializowanej wyjątku. */
    private static final long serialVersionUID = 1L;

    /**
     * Tworzy konflikt dla roli, którą konto już ma.
     *
     * @param role Nazwa zapraszanej roli
     */
    public RoleAlreadyGranted(final String role) {
        super("Konto ma już zapraszaną rolę", Map.entry("role", role), RoleAlreadyGranted.TYPE);
    }
}
