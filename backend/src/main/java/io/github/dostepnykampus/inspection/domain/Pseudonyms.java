/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.domain;

/**
 * Port pseudonimów autorów znaczników.
 *
 * <p>Wymagania: BR-5</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface Pseudonyms {

    /**
     * Daje pseudonim konta, ten sam dla tego samego konta, bez danych osobowych.
     *
     * @param account Identyfikator konta autora
     * @return Pseudonim autora
     */
    String of(String account);
}
