/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.shared.error;

/**
 * Brak logowania przy żądaniu, które go wymaga, np. lista własnych zgłoszeń.
 *
 * <p>Obsługa błędów HTTP zamienia ten typ na 401
 * {@code application/problem+json} z komunikatem w {@code detail}, jak
 * pozostałe wspólne typy błędów. Typ nie zależy od Spring Web ani
 * Jacksona.</p>
 *
 * @since 0.1.0
 */
public class MissingLogin extends RuntimeException {

    /**
     * Wyjątek, który w obsłudze żądania daje odpowiedź 401.
     *
     * @param detail Opis dla klienta, trafia bez zmian do pola
     *  {@code detail} odpowiedzi; bez szczegółów wewnętrznych
     */
    public MissingLogin(final String detail) {
        super(detail);
    }
}
