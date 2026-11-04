/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

/**
 * Brak zasobu, o który pyta żądanie, np. budynku albo zgłoszenia.
 *
 * <p>Obsługa błędów HTTP zamienia ten typ i jego podtypy (wyjątki domenowe
 * modułów) na 404 {@code application/problem+json} z komunikatem
 * w {@code detail}, więc komunikat nie zawiera szczegółów wewnętrznych.
 * Typ nie zależy od Spring Web ani Jacksona.</p>
 *
 * @since 0.1.0
 */
public class MissingResource extends RuntimeException {

    /**
     * Wyjątek, który w obsłudze żądania daje odpowiedź 404.
     *
     * @param detail Opis dla klienta, trafia bez zmian do pola
     *  {@code detail} odpowiedzi; bez szczegółów wewnętrznych
     */
    public MissingResource(final String detail) {
        super(detail);
    }
}
