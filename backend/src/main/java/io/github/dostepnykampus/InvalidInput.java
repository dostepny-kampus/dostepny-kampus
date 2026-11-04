/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

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

    public InvalidInput(final String detail) {
        super(detail);
    }
}
