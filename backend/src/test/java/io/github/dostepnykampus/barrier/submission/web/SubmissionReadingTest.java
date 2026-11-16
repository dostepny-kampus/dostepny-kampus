/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import io.github.dostepnykampus.barrier.submission.Reader;
import io.github.dostepnykampus.barrier.submission.SimilarSubmission;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import io.github.dostepnykampus.barrier.submission.SubmissionQuery;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.application.SubmissionReading;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.json.SubmissionContentJson;
import io.github.dostepnykampus.shared.error.ConflictingState;
import io.github.dostepnykampus.shared.error.MissingResource;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Zmiana zgłoszenia przyjętego: zastępowane zgłoszenie musi istnieć dla składającego
 * i być przyjęte, a ponowne wysłanie zapisanej zmiany nie jest sprawdzane.
 *
 * <p>Wymagania: BR-3, BR-5, BR-18, BR-9</p>
 *
 * @since 0.1.0
 */
final class SubmissionReadingTest {

    /**
     * Składający zmianę.
     */
    private static final Reader READER = new Reader("konto", false);

    @Test
    void skipsCheckForSubmissionWithoutReplaces() {
        Assertions.assertDoesNotThrow(
            () -> SubmissionReadingTest.request(WebBodies.ID, null).checkReplaced(
                SubmissionReadingTest.reading(null, null),
                SubmissionReadingTest.READER
            ),
            "Zgłoszenie bez wskazania zastępowanego pomija sprawdzenie"
        );
    }

    @Test
    void skipsCheckForSavedChange() {
        Assertions.assertDoesNotThrow(
            () -> SubmissionReadingTest.request(WebBodies.ID, WebBodies.PATH).checkReplaced(
                SubmissionReadingTest.reading(
                    SubmissionReadingTest.stored(SubmissionStatus.SENT),
                    null
                ),
                SubmissionReadingTest.READER
            ),
            "Zmiana ze zgłoszeniem już zapisanym pomija sprawdzenie"
        );
    }

    @Test
    void passesChangeOfAccepted() {
        Assertions.assertDoesNotThrow(
            () -> SubmissionReadingTest.request(WebBodies.ID, WebBodies.PATH).checkReplaced(
                SubmissionReadingTest.reading(
                    null,
                    SubmissionReadingTest.stored(SubmissionStatus.ACCEPTED)
                ),
                SubmissionReadingTest.READER
            ),
            "Zmiana przyjętego zgłoszenia przechodzi sprawdzenie"
        );
    }

    @Test
    void checksReplacedForChangeWithoutId() {
        Assertions.assertThrows(
            ConflictingState.class,
            () -> SubmissionReadingTest.request(null, WebBodies.PATH).checkReplaced(
                SubmissionReadingTest.reading(
                    null,
                    SubmissionReadingTest.stored(SubmissionStatus.SENT)
                ),
                SubmissionReadingTest.READER
            ),
            "Zmiana bez identyfikatora sprawdza zastępowane zgłoszenie i daje konflikt"
        );
    }

    @Test
    void givesNotFoundForChangeOfMissing() {
        final NewSubmissionRequest request =
            SubmissionReadingTest.request(WebBodies.ID, WebBodies.PATH);
        Assertions.assertEquals(
            "Nie ma zastępowanego zgłoszenia",
            Assertions.assertThrows(
                MissingResource.class,
                () -> request.checkReplaced(
                    SubmissionReadingTest.reading(null, null),
                    SubmissionReadingTest.READER
                ),
                "Zmiana brakującego zgłoszenia kończy się brakiem zasobu"
            ).getMessage(),
            "Brak zastępowanego zgłoszenia jest opisany w komunikacie"
        );
    }

    @Test
    void givesConflictForChangeOfUnaccepted() {
        Assertions.assertThrows(
            ConflictingState.class,
            () -> SubmissionReadingTest.request(WebBodies.ID, WebBodies.PATH).checkReplaced(
                SubmissionReadingTest.reading(
                    null,
                    SubmissionReadingTest.stored(SubmissionStatus.IN_REVIEW)
                ),
                SubmissionReadingTest.READER
            ),
            "Zmiana nieprzyjętego zgłoszenia daje konflikt"
        );
    }

    private static NewSubmissionRequest request(
        final @Nullable UUID id, final @Nullable UUID replaces
    ) {
        final SubmissionContent content = WebBodies.content();
        return new NewSubmissionRequest(
            id,
            new SubmissionContentJson(
                new SubmissionContent(
                    content.kind(), content.place(), content.barrier(), null, null, replaces,
                    content.photoIds()
                )
            )
        );
    }

    private static SubmissionReading reading(
        final @Nullable Submission saved, final @Nullable Submission visible
    ) {
        return new SubmissionReading(new Finder(saved, visible), symbol -> false);
    }

    private static Submission stored(final SubmissionStatus status) {
        return new Submission(
            WebBodies.PATH, 1, status, Instant.parse("2026-03-01T10:00:00Z"), WebBodies.content()
        );
    }

    /**
     * Odczyt zgłoszeń z jednym zapisanym zgłoszeniem i jednym widocznym dla składającego.
     *
     * @param saved Zgłoszenie zapisane pod identyfikatorem z ciała albo null
     * @param visible Zastępowane zgłoszenie widoczne dla składającego albo null
     * @since 0.1.0
     */
    private record Finder(@Nullable Submission saved, @Nullable Submission visible)
        implements SubmissionFinder {

        @Override
        public Optional<Submission> find(final UUID id) {
            return Optional.ofNullable(this.saved).filter(found -> WebBodies.ID.equals(id));
        }

        @Override
        public Optional<Submission> visible(final UUID id, final Reader reader) {
            return Optional.ofNullable(this.visible).filter(
                found -> WebBodies.PATH.equals(id)
                    && reader.equals(SubmissionReadingTest.READER)
            );
        }

        @Override
        public boolean foreign(final UUID id, final String account) {
            throw new UnsupportedOperationException("Sprawdzenie nie pyta o cudze");
        }

        @Override
        public boolean claimedBy(final UUID id, final String account) {
            throw new UnsupportedOperationException("Sprawdzenie zmiany nie pyta o Weryfikatora");
        }

        @Override
        public boolean authorOnly(final UUID id, final String account) {
            throw new UnsupportedOperationException("Sprawdzenie zmiany nie pyta o autora");
        }

        @Override
        public SubmissionFinder.Page page(
            final SubmissionQuery query, final UUID after, final int limit
        ) {
            throw new UnsupportedOperationException("Sprawdzenie nie czyta stron listy");
        }

        @Override
        public SubmissionFinder.Page own(
            final String author, final SubmissionQuery query, final UUID after, final int limit
        ) {
            throw new UnsupportedOperationException("Sprawdzenie nie czyta własnych");
        }

        @Override
        public List<SimilarSubmission> similar(final UUID id) {
            throw new UnsupportedOperationException("Sprawdzenie zmiany nie szuka podobnych");
        }
    }
}
