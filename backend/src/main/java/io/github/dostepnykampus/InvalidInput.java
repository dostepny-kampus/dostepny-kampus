/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import org.jspecify.annotations.Nullable;

/**
 * Niepoprawne dane żądania: wartość, której serwer nie przyjmuje.
 *
 * <p>Obsługa błędów HTTP zamienia ten typ i jego podtypy (wyjątki domenowe
 * modułów) na 400 {@code application/problem+json} z komunikatem
 * w {@code detail}, więc komunikat nie zawiera szczegółów wewnętrznych.
 * Typ nie zależy od Spring Web ani Jacksona.</p>
 *
 * @since 0.1.0
 */
public class InvalidInput extends RuntimeException {

    /**
     * Wyjątek, który w obsłudze żądania daje odpowiedź 400.
     *
     * @param detail Opis dla klienta, trafia bez zmian do pola
     *  {@code detail} odpowiedzi; bez szczegółów wewnętrznych
     */
    public InvalidInput(final String detail) {
        this(detail, null);
    }

    /**
     * Wyjątek z przyczyną, która trafia tylko do dziennika.
     *
     * @param detail Opis dla klienta
     * @param cause Przyczyna albo null
     */
    public InvalidInput(final String detail, final @Nullable Throwable cause) {
        super(detail, cause);
    }
}
