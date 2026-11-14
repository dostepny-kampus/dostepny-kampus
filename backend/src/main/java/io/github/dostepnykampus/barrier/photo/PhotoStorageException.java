/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

/**
 * Magazyn zdjęć jest niedostępny albo odrzucił operację; przyczyną jest błąd magazynu.
 *
 * <p>Wymagania: BR-9</p>
 *
 * @since 0.1.0
 */
public final class PhotoStorageException extends RuntimeException {

    /**
     * Wersja serializacji wyjątku.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Tworzy błąd magazynu zdjęć.
     *
     * @param message Opis operacji, która się nie udała
     * @param cause Błąd magazynu
     */
    public PhotoStorageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
