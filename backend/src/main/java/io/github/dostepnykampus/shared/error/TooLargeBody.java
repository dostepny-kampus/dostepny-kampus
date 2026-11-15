/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.shared.error;

/**
 * Ciało żądania większe niż limit serwera; daje odpowiedź 413 z opisem w {@code detail}.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
public final class TooLargeBody extends RuntimeException {

    /**
     * Wersja postaci serializowanej.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Przekroczenie limitu z opisem dla klienta.
     *
     * @param detail Opis limitu
     */
    public TooLargeBody(final String detail) {
        super(detail);
    }
}
