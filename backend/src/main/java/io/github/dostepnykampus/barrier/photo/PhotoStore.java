/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Magazyn zdjęć zgłoszeń: jedno zdjęcie na parę identyfikatorów zgłoszenia i zdjęcia nadanych
 * w telefonie. Każda metoda rzuca {@link PhotoStorageException}, gdy magazyn jest niedostępny
 * albo odrzuca operację.
 *
 * <p>Wymagania: BR-9</p>
 *
 * @since 0.1.0
 */
public interface PhotoStore {

    /**
     * Zapisuje zdjęcie; ponowny zapis tego samego zdjęcia je zastępuje.
     *
     * @param submission Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia
     * @param jpeg Plik JPEG
     */
    void put(UUID submission, UUID photo, byte[] jpeg);

    /**
     * Czyta zdjęcie.
     *
     * @param submission Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia
     * @return Plik JPEG; brak, gdy magazyn nie ma zdjęcia
     */
    Optional<byte[]> get(UUID submission, UUID photo);

    /**
     * Zdjęcia zgłoszenia zapisane w magazynie, także spoza bieżącej listy photoIds.
     *
     * @param submission Identyfikator zgłoszenia
     * @return Identyfikatory zapisanych zdjęć zgłoszenia
     */
    List<UUID> stored(UUID submission);

    /**
     * Usuwa zdjęcie; brak zdjęcia nie jest błędem.
     *
     * @param submission Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia
     */
    void delete(UUID submission, UUID photo);
}
