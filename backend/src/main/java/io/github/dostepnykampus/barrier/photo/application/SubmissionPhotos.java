/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo.application;

import io.github.dostepnykampus.barrier.photo.JpegCleaner;
import io.github.dostepnykampus.barrier.photo.PhotoStorageException;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import io.github.dostepnykampus.barrier.photo.PhotoUpload;
import io.github.dostepnykampus.barrier.review.ReviewRepository;
import io.github.dostepnykampus.barrier.submission.EditResult;
import io.github.dostepnykampus.barrier.submission.Reader;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.application.SubmissionService;
import io.github.dostepnykampus.shared.error.ForbiddenChange;
import io.github.dostepnykampus.shared.error.InvalidInput;
import io.github.dostepnykampus.shared.error.MissingResource;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Zdjęcia zgłoszeń: zapis zdjęcia z listy photoIds i odczyt
 * zdjęcia zgłoszenia przyjętego.
 *
 * <p>Plik trafia do magazynu poza transakcją; pod blokadą wiersza zgłoszenia
 * zostają tylko kontrola statusu i przeniesienie pliku pod zdjęcie.</p>
 *
 * <p>Po zatwierdzonej edycji zdjęcia spoza nowej listy photoIds znikają z
 * magazynu, więc nie zostają dane, których nikt nie może usunąć.</p>
 *
 * <p>Wymagania: BR-7, BR-8, US2.2</p>
 *
 * @since 0.1.0
 */
@Service
public final class SubmissionPhotos {

    /** Dziennik błędów usuwania starych zdjęć. */
    private static final Logger LOG = LoggerFactory.getLogger(SubmissionPhotos.class);

    /** Zgłoszenia z listą zdjęć. */
    private final SubmissionFinder submissions;

    /** Magazyn zdjęć. */
    private final PhotoStore photos;

    /** Blokada wiersza podczas kontroli statusu i zapisu zdjęcia. */
    private final SubmissionService service;

    /** Weryfikator, który wziął zgłoszenie. */
    private final ReviewRepository reviews;

    /**
     * Tworzy usługę zdjęć zgłoszeń.
     *
     * @param submissions Odczyt zgłoszeń
     * @param photos Magazyn zdjęć
     * @param service Blokada wiersza podczas kontroli statusu i zapisu zdjęcia
     * @param reviews Weryfikator, który wziął zgłoszenie
     */
    public SubmissionPhotos(
        final SubmissionFinder submissions, final PhotoStore photos,
        final SubmissionService service, final ReviewRepository reviews
    ) {
        this.submissions = submissions;
        this.photos = photos;
        this.service = service;
        this.reviews = reviews;
    }

    /**
     * Zapisuje zdjęcie od konta z tokenu. Weryfikator, który wziął zgłoszenie w weryfikacji,
     * zastępuje zapisane zdjęcie, np. wersją z zasłoniętą twarzą, bez nowej wersji zgłoszenia.
     * Inne konto niż autor dostaje odmowę, a autor zapisuje zdjęcie jak w {@link #put(UUID,
     * UUID, PhotoUpload)}.
     *
     * <p>Wymagania: NFR-25, BR-15, BR-3</p>
     *
     * @param write Zdjęcie zgłoszenia i konto, które je wysyła
     * @param upload Ciało żądania z JPEG
     * @throws IOException Gdy odczyt ciała się nie uda albo przekracza limit
     *  rozmiaru (PhotoTooLargeException)
     */
    public void put(final PhotoWrite write, final PhotoUpload upload) throws IOException {
        final UUID submission = write.submission();
        final UUID photo = write.photo();
        if (this.reviewing(submission, write.account())) {
            this.find(submission, photo);
            this.write(
                submission, photo, upload, () -> {
                    if (!this.reviewing(submission, write.account())) {
                        throw new ForbiddenChange(
                            "Zdjęcie zastępuje tylko Weryfikator, który wziął zgłoszenie"
                        );
                    }
                }
            );
        } else {
            this.author(submission, write.account());
            this.put(submission, photo, upload);
        }
    }

    /**
     * Zapisuje zdjęcie bez metadanych; ponowne wysłanie tego samego
     * zdjęcia je zastępuje, ale w weryfikacji zapisanego zdjęcia autor nie zastępuje.
     *
     * <p>Wymagania: NFR-26, BR-9, NFR-25</p>
     *
     * @param submission Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia z listy photoIds
     * @param upload Ciało żądania z JPEG
     * @throws IOException Gdy odczyt ciała się nie uda albo przekracza limit
     *  rozmiaru (PhotoTooLargeException)
     */
    public void put(final UUID submission, final UUID photo, final PhotoUpload upload)
        throws IOException {
        this.editable(submission, photo);
        this.write(submission, photo, upload, () -> this.replaceable(submission, photo));
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
    public byte[] get(final UUID submission, final UUID photo, final Reader reader) {
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
        } catch (final PhotoStorageException ex) {
            SubmissionPhotos.LOG.warn(
                String.format("Nie usunięto starych zdjęć zgłoszenia %s", submission.id()), ex
            );
        }
    }

    private void write(
        final UUID submission, final UUID photo, final PhotoUpload upload, final Runnable check
    ) throws IOException {
        final UUID staged = UUID.randomUUID();
        this.photos.stage(staged, SubmissionPhotos.clean(upload.read()));
        try {
            this.service.locked(
                submission, () -> {
                    check.run();
                    this.photos.promote(staged, submission, photo);
                }
            );
        } finally {
            this.discard(staged);
        }
    }

    private boolean reviewing(final UUID submission, final String account) {
        return this.submissions.find(submission)
            .filter(item -> item.status() == SubmissionStatus.IN_REVIEW).isPresent()
            && this.reviews.claimedBy(submission, account);
    }

    private void author(final UUID submission, final String account) {
        if (this.submissions.foreign(submission, account)) {
            throw new ForbiddenChange("Zdjęcie dodaje tylko autor zgłoszenia");
        }
    }

    private void discard(final UUID staged) {
        try {
            this.photos.discard(staged);
        } catch (final PhotoStorageException ex) {
            SubmissionPhotos.LOG.warn(String.format("Nie usunięto wysyłki %s", staged), ex);
        }
    }

    private static byte[] clean(final byte[] raw) {
        try {
            return new JpegCleaner().strip(raw);
        } catch (final IOException ex) {
            throw new InvalidInput("Uszkodzony plik JPEG", ex);
        }
    }

    private Submission find(final UUID submission, final UUID photo) {
        return this.submissions.find(submission)
            .filter(item -> item.content().photoIds().contains(photo))
            .orElseThrow(() -> new MissingResource("Nie ma takiego zdjęcia zgłoszenia"));
    }

    private SubmissionStatus editable(final UUID submission, final UUID photo) {
        final SubmissionStatus status = this.find(submission, photo).status();
        if (!status.editable()) {
            throw new ForbiddenChange("Zdjęcie zgłoszenia przyjętego zmienia tylko Weryfikator");
        }
        return status;
    }

    private void replaceable(final UUID submission, final UUID photo) {
        if (this.editable(submission, photo) == SubmissionStatus.IN_REVIEW
            && this.photos.stored(submission).contains(photo)) {
            throw new ForbiddenChange(
                "Zapisanego zdjęcia zgłoszenia w weryfikacji autor nie zmienia"
            );
        }
    }
}
