/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

/**
 * Port kontroli zdjęć w magazynie: zgłoszenie bez wszystkich zdjęć z listy photoIds nie trafia
 * do weryfikacji.
 *
 * <p>Wymagania: BR-7, BR-8</p>
 *
 * @since 0.1.0
 */
public interface StoredPhotos {

    /**
     * Mówi, czy magazyn ma każde zdjęcie z listy photoIds zgłoszenia.
     *
     * @param submission Zgłoszenie
     * @return Prawda, gdy żadnego zdjęcia nie brakuje
     */
    boolean complete(Submission submission);
}
