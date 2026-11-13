/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.shared.error;


/**
 * Zmiana zasobu niedozwolona w jego stanie, np. zdjęcie zgłoszenia przyjętego.
 *
 * <p>Daje 403 {@code application/problem+json}; typ nie zależy od Spring Web.</p>
 *
 * @since 0.1.0
 */
public class ForbiddenChange extends RuntimeException {

    /**
     * Wyjątek dający odpowiedź 403.
     *
     * @param detail Opis dla klienta w polu {@code detail}, bez szczegółów wewnętrznych
     */
    public ForbiddenChange(final String detail) {
        super(detail);
    }
}
