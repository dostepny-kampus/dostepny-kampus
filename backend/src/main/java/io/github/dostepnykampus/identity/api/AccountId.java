/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.api;

import java.util.Objects;

/**
 * Identyfikator konta w Usłudze logowania, czyli {@code sub} z tokenu. Na zewnątrz serwera
 * konto przedstawia tylko pseudonim, który nie ujawnia identyfikatora.
 *
 * <p>Wymagania: BR-5</p>
 *
 * @param value Identyfikator konta
 * @since 0.1.0
 */
public record AccountId(String value) {

    /**
     * Identyfikator z wartością; brak wartości kończy się wyjątkiem NullPointerException.
     */
    public AccountId {
        Objects.requireNonNull(value, "value");
    }

    /**
     * Pseudonim konta w postaci „Autor N” z N od 1 do 999, ten sam dla tego samego konta.
     * Różne konta mogą dostać ten sam pseudonim.
     *
     * @return Pseudonim konta
     */
    public String pseudonym() {
        return String.format("Autor %d", Math.floorMod(this.value.hashCode(), 999) + 1);
    }
}
