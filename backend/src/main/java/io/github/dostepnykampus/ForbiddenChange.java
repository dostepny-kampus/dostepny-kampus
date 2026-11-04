/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

/**
 * Zmiana zasobu, której serwer nie wykona w jego bieżącym stanie, np. zdjęcie
 * zgłoszenia już przyjętego.
 *
 * <p>Obsługa błędów HTTP zamienia ten typ na 403
 * {@code application/problem+json} z komunikatem w {@code detail}, więc
 * komunikat nie zawiera szczegółów wewnętrznych. Typ nie zależy od Spring Web
 * ani Jacksona.</p>
 *
 * @since 0.1.0
 */
public class ForbiddenChange extends RuntimeException {

    /**
     * Wersja serializacji wyjątku.
     */
    private static final long serialVersionUID = 1L;

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
