/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo.application;

import io.github.dostepnykampus.barrier.photo.PhotoStore;
import io.github.dostepnykampus.barrier.submission.StoredPhotos;
import io.github.dostepnykampus.barrier.submission.Submission;
import org.springframework.stereotype.Service;

/**
 * Kontrola zdjęć zgłoszenia według listy obiektów w magazynie.
 *
 * <p>Wymagania: BR-7, BR-8</p>
 *
 * @since 0.1.0
 */
@Service
public final class CompletePhotos implements StoredPhotos {

    /** Magazyn zdjęć. */
    private final PhotoStore photos;

    /**
     * Tworzy kontrolę zdjęć.
     *
     * @param photos Magazyn zdjęć
     */
    public CompletePhotos(final PhotoStore photos) {
        this.photos = photos;
    }

    @Override
    public boolean complete(final Submission submission) {
        return this.photos.stored(submission.id()).containsAll(submission.content().photoIds());
    }
}
