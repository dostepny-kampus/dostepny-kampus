/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.net.URI;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Niepoprawne dane żądania: wartość, której serwer nie przyjmuje.
 *
 * <p>Obsługa błędów HTTP zamienia ten typ i jego podtypy (wyjątki domenowe
 * modułów) na 400 {@code application/problem+json} z komunikatem
 * w {@code detail}, więc komunikat nie zawiera szczegółów wewnętrznych.
 * Błąd, po którym aplikacja pokazuje osobny stan ekranu, ma też rodzaj
 * w {@code type} (RFC 9457), np. zgłoszenie spoza terenu PWr.
 * Typ nie zależy od Spring Web ani Jacksona.</p>
 *
 * <p>Wymagania: BR-G5</p>
 *
 * @since 0.1.0
 */
public class InvalidInput extends RuntimeException {

    /**
     * Identyfikator postaci serializowanej wyjątku.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Rodzaj błędu do pola {@code type} odpowiedzi albo null.
     */
    private final @Nullable URI kind;

    /**
     * Wyjątek, który w obsłudze żądania daje odpowiedź 400.
     *
     * @param detail Opis dla klienta, trafia bez zmian do pola
     *  {@code detail} odpowiedzi; bez szczegółów wewnętrznych
     */
    public InvalidInput(final String detail) {
        this(detail, null, null);
    }

    /**
     * Wyjątek z przyczyną, która trafia tylko do dziennika.
     *
     * @param detail Opis dla klienta
     * @param cause Przyczyna albo null
     */
    public InvalidInput(final String detail, final @Nullable Throwable cause) {
        this(detail, cause, null);
    }

    /**
     * Wyjątek z rodzajem błędu, po którym klient rozpozna przypadek.
     *
     * @param detail Opis dla klienta
     * @param kind Rodzaj błędu do pola {@code type} odpowiedzi
     */
    protected InvalidInput(final String detail, final URI kind) {
        this(detail, null, kind);
    }

    private InvalidInput(
        final String detail, final @Nullable Throwable cause, final @Nullable URI kind
    ) {
        super(detail, cause);
        this.kind = kind;
    }

    /**
     * Rodzaj błędu do pola {@code type} odpowiedzi.
     *
     * @return Rodzaj; brak dla zwykłego błędu danych
     */
    public final Optional<URI> type() {
        return Optional.ofNullable(this.kind);
    }
}
