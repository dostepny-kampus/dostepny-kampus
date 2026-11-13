/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.io.IOException;

/**
 * Wejściowe zdjęcie przekracza limit rozmiaru; nie wolno go przetwarzać ani zapisywać.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class PhotoTooLargeException extends IOException {

    /**
     * Wersja serializacji wyjątku.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Tworzy błąd przekroczenia pięciu megabajtów.
     */
    PhotoTooLargeException() {
        super("Zdjęcie przekracza limit 5 MB");
    }
}
