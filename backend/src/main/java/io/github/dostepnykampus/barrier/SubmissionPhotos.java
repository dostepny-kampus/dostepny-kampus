/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.ForbiddenChange;
import io.github.dostepnykampus.InvalidInput;
import io.github.dostepnykampus.MissingResource;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionFinder;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Zdjęcia zgłoszeń: zapis zdjęcia z listy photoIds (BR-7, BR-8) i odczyt
 * zdjęcia zgłoszenia przyjętego (US2.2).
 *
 * <p>Status jest sprawdzany przed zapisem do S3, bez wspólnej transakcji z bazą:
 * zgłoszenie może zostać przyjęte pomiędzy sprawdzeniem a zapisem i zdjęcie je
 * zastąpi. Wyścig przyjęty do sprintu 5 (weryfikator); S3 i SQL nie są atomowe.</p>
 *
 * @since 0.1.0
 */
@Service
final class SubmissionPhotos {

    /** Zgłoszenia z listą zdjęć. */
    private final SubmissionFinder submissions;

    /** Magazyn zdjęć. */
    private final PhotoStore photos;

    SubmissionPhotos(final SubmissionFinder submissions, final PhotoStore photos) {
        this.submissions = submissions;
        this.photos = photos;
    }

    /**
     * Zapisuje zdjęcie bez metadanych (NFR-26); ponowne wysłanie tego samego
     * zdjęcia je zastępuje (BR-9).
     *
     * @param submission Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia z listy photoIds
     * @param upload Ciało żądania z JPEG
     * @throws IOException Gdy odczyt ciała się nie uda albo przekracza limit
     *  NFR-34 (PhotoTooLargeException)
     */
    void put(final UUID submission, final UUID photo, final PhotoUpload upload)
        throws IOException {
        final Submission found = this.find(submission, photo);
        if (!found.status().editable()) {
            throw new ForbiddenChange("Zdjęcie zgłoszenia przyjętego zmienia tylko Weryfikator");
        }
        this.photos.put(submission, photo, SubmissionPhotos.clean(upload.read()));
    }

    /**
     * Czyta zdjęcie zgłoszenia przyjętego (US2.2, BR-6).
     *
     * @param submission Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia z listy photoIds
     * @return Plik JPEG bez metadanych
     */
    byte[] get(final UUID submission, final UUID photo) {
        return Optional.of(this.find(submission, photo))
            .filter(item -> item.status().publiclyVisible())
            .flatMap(item -> this.photos.get(submission, photo))
            .orElseThrow(() -> new MissingResource("Nie ma takiego zdjęcia zgłoszenia"));
    }

    private static byte[] clean(final byte[] raw) {
        try {
            return JpegCleaner.strip(raw);
        } catch (final IOException ex) {
            throw new InvalidInput("Uszkodzony plik JPEG", ex);
        }
    }

    private Submission find(final UUID submission, final UUID photo) {
        return this.submissions.find(submission)
            .filter(item -> item.content().photoIds().contains(photo))
            .orElseThrow(() -> new MissingResource("Nie ma takiego zdjęcia zgłoszenia"));
    }
}
