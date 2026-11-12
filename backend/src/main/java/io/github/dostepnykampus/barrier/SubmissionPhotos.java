/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.ForbiddenChange;
import io.github.dostepnykampus.InvalidInput;
import io.github.dostepnykampus.MissingResource;
import io.github.dostepnykampus.barrier.domain.EditResult;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionFinder;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.core.exception.SdkException;

/**
 * Zdjęcia zgłoszeń: zapis zdjęcia z listy photoIds (BR-7, BR-8) i odczyt
 * zdjęcia zgłoszenia przyjętego (US2.2).
 *
 * <p>Status jest sprawdzany przed zapisem do S3, bez wspólnej transakcji z bazą:
 * zgłoszenie może zostać przyjęte pomiędzy sprawdzeniem a zapisem i zdjęcie je
 * zastąpi. Wyścig przyjęty do sprintu 5 (weryfikator); S3 i SQL nie są atomowe.</p>
 *
 * <p>Po zatwierdzonej edycji zdjęcia spoza nowej listy photoIds znikają z
 * magazynu, więc nie zostają dane, których nikt nie może usunąć (#839).</p>
 *
 * @since 0.1.0
 */
@Service
final class SubmissionPhotos {

    /** Dziennik błędów usuwania starych zdjęć. */
    private static final Logger LOG = LoggerFactory.getLogger(SubmissionPhotos.class);

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

    /**
     * Usuwa z magazynu zdjęcia spoza listy photoIds zapisanej edycji. Działa
     * po zatwierdzeniu transakcji; błąd magazynu nie cofa edycji, trafia do
     * logu (#839).
     *
     * @param saved Zapisana edycja zgłoszenia
     */
    @TransactionalEventListener
    void prune(final EditResult.Saved saved) {
        final Submission submission = saved.submission();
        final List<UUID> kept = submission.content().photoIds();
        try {
            this.photos.stored(submission.id()).stream()
                .filter(photo -> !kept.contains(photo))
                .forEach(photo -> this.photos.delete(submission.id(), photo));
        } catch (final SdkException ex) {
            SubmissionPhotos.LOG.warn(
                String.format("Nie usunięto starych zdjęć zgłoszenia %s", submission.id()), ex
            );
        }
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
