/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

/**
 * Zmiana zasobu niedozwolona w jego stanie, np. zdjęcie zgłoszenia przyjętego.
 *
 * <p>Obsługa błędów HTTP zamienia ten typ na 403 {@code application/problem+json}
 * z komunikatem w {@code detail}. Typ nie zależy od Spring Web ani Jacksona.</p>
 *
 * @since 0.1.0
 */
public class ForbiddenChange extends RuntimeException {

    /**
     * Wyjątek, który w obsłudze żądania daje odpowiedź 403.
     *
     * @param detail Opis dla klienta, trafia bez zmian do pola
     *  {@code detail} odpowiedzi; bez szczegółów wewnętrznych
     */
    public ForbiddenChange(final String detail) {
        super(detail);
    }
}
