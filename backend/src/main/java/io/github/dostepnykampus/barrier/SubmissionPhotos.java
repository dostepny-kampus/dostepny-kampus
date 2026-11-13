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
import io.github.dostepnykampus.barrier.domain.Reader;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionFinder;
import io.github.dostepnykampus.barrier.domain.SubmissionRepository;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.core.exception.SdkException;

/**
 * Zdjęcia zgłoszeń: zapis zdjęcia z listy photoIds i odczyt
 * zdjęcia zgłoszenia przyjętego.
 *
 * <p>Kontrola statusu i zapis do S3 utrzymują blokadę wiersza zgłoszenia,
 * więc przyjęcie lub edycja treści czekają na zakończenie zapisu zdjęcia.</p>
 *
 * <p>Po zatwierdzonej edycji zdjęcia spoza nowej listy photoIds znikają z
 * magazynu, więc nie zostają dane, których nikt nie może usunąć.</p>
 *
 * <p>Wymagania: BR-7, BR-8, US2.2</p>
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

    /** Blokada wiersza podczas kontroli statusu i zapisu zdjęcia. */
    private final SubmissionRepository repository;

    /** Transakcja obejmująca kontrolę statusu i zapis zdjęcia. */
    private final TransactionTemplate transaction;

    SubmissionPhotos(
        final SubmissionFinder submissions, final PhotoStore photos,
        final SubmissionRepository repository, final PlatformTransactionManager manager
    ) {
        this.submissions = submissions;
        this.photos = photos;
        this.repository = repository;
        this.transaction = new TransactionTemplate(manager);
    }

    /**
     * Odmawia kontu, które nie jest autorem zgłoszenia: zdjęcie dodaje tylko
     * autor. Brak zgłoszenia zostawia dalszej obsłudze.
     *
     * <p>Wymagania: BR-3</p>
     *
     * @param submission Identyfikator zgłoszenia
     * @param account Identyfikator konta z tokenu
     */
    void author(final UUID submission, final String account) {
        if (this.submissions.foreign(submission, account)) {
            throw new ForbiddenChange("Zdjęcie dodaje tylko autor zgłoszenia");
        }
    }

    /**
     * Zapisuje zdjęcie bez metadanych; ponowne wysłanie tego samego
     * zdjęcia je zastępuje.
     *
     * <p>Wymagania: NFR-26, BR-9</p>
     *
     * @param submission Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia z listy photoIds
     * @param upload Ciało żądania z JPEG
     * @throws IOException Gdy odczyt ciała się nie uda albo przekracza limit
     *  rozmiaru (PhotoTooLargeException)
     */
    void put(final UUID submission, final UUID photo, final PhotoUpload upload)
        throws IOException {
        this.editable(submission, photo);
        final byte[] jpeg = SubmissionPhotos.clean(upload.read());
        this.transaction.executeWithoutResult(status -> {
            this.repository.lock(submission);
            this.editable(submission, photo);
            this.photos.put(submission, photo, jpeg);
        });
    }

    /**
     * Czyta zdjęcie zgłoszenia, które czytelnik może zobaczyć: przyjętego
     * każdy, pozostałych autor i Weryfikator.
     *
     * <p>Wymagania: US2.2, BR-6, BR-5</p>
     *
     * @param submission Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia z listy photoIds
     * @param reader Osoba, która czyta zdjęcie
     * @return Plik JPEG bez metadanych
     */
    byte[] get(final UUID submission, final UUID photo, final Reader reader) {
        return this.submissions.visible(submission, reader)
            .filter(item -> item.content().photoIds().contains(photo))
            .flatMap(item -> this.photos.get(submission, photo))
            .orElseThrow(() -> new MissingResource("Nie ma takiego zdjęcia zgłoszenia"));
    }

    /**
     * Usuwa z magazynu zdjęcia spoza listy photoIds zapisanej edycji. Działa
     * po zatwierdzeniu transakcji; błąd magazynu nie cofa edycji, trafia do
     * logu.
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

    private void editable(final UUID submission, final UUID photo) {
        if (!this.find(submission, photo).status().editable()) {
            throw new ForbiddenChange("Zdjęcie zgłoszenia przyjętego zmienia tylko Weryfikator");
        }
    }
}
