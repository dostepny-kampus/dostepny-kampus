/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.review.application;

import io.github.dostepnykampus.barrier.review.ReviewRepository;
import io.github.dostepnykampus.barrier.review.SimilarSubmission;
import io.github.dostepnykampus.barrier.submission.StoredPhotos;
import io.github.dostepnykampus.barrier.submission.Submission;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Kolejka weryfikacji i podobne zgłoszenia dla warstwy web.
 *
 * <p>Wymagania: US10.1, US10.4, US10.5, US10.7, BR-4, BR-7, BR-11, BR-13</p>
 *
 * @since 0.1.0
 */
@Service
public final class ReviewQueue {

    /** Port weryfikacji zgłoszeń. */
    private final ReviewRepository reviews;

    /** Port kontroli zdjęć w magazynie. */
    private final StoredPhotos photos;

    /**
     * Ctor.
     *
     * @param reviews Port weryfikacji zgłoszeń
     * @param photos Port kontroli zdjęć w magazynie
     */
    public ReviewQueue(final ReviewRepository reviews, final StoredPhotos photos) {
        this.reviews = reviews;
        this.photos = photos;
    }

    /**
     * Wyszukuje zgłoszenia w kolejce weryfikacji dla danego konta Weryfikatora; zgłoszenie,
     * któremu brakuje zdjęcia w magazynie, jeszcze do niej nie trafia.
     *
     * @param reviewer Identyfikator konta Weryfikatora
     * @return Zgłoszenia w kolejce od najdawniej wysłanego
     */
    public List<Submission> forReviewer(final String reviewer) {
        return this.reviews.reviewQueue(reviewer).stream().filter(this.photos::complete).toList();
    }

    /**
     * Wyszukuje zgłoszenia podobne do danego.
     *
     * <p>Wymagania: US10.1, BR-13</p>
     *
     * @param id Identyfikator sprawdzanego zgłoszenia
     * @return Podobne zgłoszenia od najbliższego
     */
    public List<SimilarSubmission> similar(final UUID id) {
        return this.reviews.similar(id);
    }
}
