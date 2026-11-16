/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo.application;

import io.github.dostepnykampus.barrier.photo.PhotoStorageException;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import io.github.dostepnykampus.barrier.submission.SubmissionsPurged;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Usuwa z magazynu zdjęcia zgłoszeń usuniętych po okresie przechowywania. Działa po
 * zatwierdzeniu transakcji; błąd magazynu przy jednym zgłoszeniu trafia do logu i nie
 * zatrzymuje pozostałych.
 *
 * <p>Wymagania: NFR-29</p>
 *
 * @since 0.1.0
 */
@Service
public class PurgedPhotos {

    /** Dziennik błędów usuwania zdjęć. */
    private static final Logger LOG = LoggerFactory.getLogger(PurgedPhotos.class);

    /** Magazyn zdjęć. */
    private final PhotoStore photos;

    PurgedPhotos(final PhotoStore photos) {
        this.photos = photos;
    }

    /**
     * Usuwa wszystkie zdjęcia usuniętych zgłoszeń.
     *
     * @param purged Usunięte zgłoszenia
     */
    @TransactionalEventListener
    public void delete(final SubmissionsPurged purged) {
        purged.ids().forEach(this::deleteAll);
    }

    private void deleteAll(final UUID submission) {
        try {
            this.photos.stored(submission).forEach(photo -> this.photos.delete(submission, photo));
        } catch (final PhotoStorageException ex) {
            PurgedPhotos.LOG.warn(
                String.format("Nie usunięto zdjęć usuniętego zgłoszenia %s", submission), ex
            );
        }
    }
}
