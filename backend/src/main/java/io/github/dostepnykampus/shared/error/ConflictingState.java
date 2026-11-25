/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.shared.error;

import java.net.URI;
import java.util.Optional;

/**
 * Konflikt z bieżącym stanem zasobu na serwerze, np. nieaktualna wersja zgłoszenia.
 * Obsługa błędów HTTP zamienia ten typ na 409 {@code application/problem+json}
 * z komunikatem w {@code detail} i bieżącym stanem we właściwości {@link #property()}
 * na najwyższym poziomie odpowiedzi. Konflikt z osobnym stanem ekranu w aplikacji ma też
 * rodzaj w {@code type}. Typ nie zależy od Spring Web ani Jacksona.
 *
 * @since 0.1.0
 */
public class ConflictingState extends RuntimeException {

    /**
     * Nazwa właściwości odpowiedzi z bieżącym stanem.
     */
    private final String property;

    /**
     * Bieżący stan zasobu, serializowany jako obiekt JSON.
     */
    private final transient Object value;

    /**
     * Wyjątek, który w obsłudze żądania daje odpowiedź 409.
     *
     * @param detail Opis dla klienta, trafia bez zmian do pola
     *  {@code detail} odpowiedzi; bez szczegółów wewnętrznych
     * @param property Nazwa właściwości odpowiedzi z bieżącym stanem
     * @param value Bieżący stan zasobu, np. rekord odpowiedzi API
     */
    public ConflictingState(final String detail, final String property, final Object value) {
        super(detail);
        this.property = property;
        this.value = value;
    }

    /**
     * Nazwa właściwości odpowiedzi z bieżącym stanem.
     *
     * @return Nazwa właściwości, np. {@code serverSubmission}
     */
    public String property() {
        return this.property;
    }

    /**
     * Bieżący stan zasobu.
     *
     * @return Wartość właściwości {@link #property()}
     */
    public Object value() {
        return this.value;
    }

    /**
     * Rodzaj błędu do pola {@code type} odpowiedzi.
     *
     * @return Rodzaj; brak dla zwykłego konfliktu
     */
    public Optional<URI> type() {
        return Optional.empty();
    }
}
