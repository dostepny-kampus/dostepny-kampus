/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.domain.Reader;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionFinder;
import io.github.dostepnykampus.barrier.json.SubmissionContentJson;
import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import io.github.dostepnykampus.shared.error.ConflictingState;
import io.github.dostepnykampus.shared.error.MissingResource;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Ciało POST /submissions, schemat NewSubmission w OpenAPI.
 *
 * @param id Identyfikator zgłoszenia nadany przez klienta albo null, gdy nie podano
 * @param content Treść zgłoszenia, w JSON płasko obok identyfikatora
 * @since 0.1.0
 */
record NewSubmissionRequest(
    @Nullable UUID id, @JsonUnwrapped SubmissionContentJson content
) {

    /**
     * Przenosi ciało żądania do komendy domeny.
     *
     * @param author Identyfikator konta z tokenu
     * @return Komenda utworzenia zgłoszenia
     */
    NewSubmission toCommand(final String author) {
        if (this.id == null) {
            throw new InvalidSubmission("Brak identyfikatora zgłoszenia w polu id");
        }
        return new NewSubmission(this.id, this.content.toContent(), author);
    }

    /**
     * Sprawdza zgłoszenie zastępowane przez zmianę: brak
     * albo zgłoszenie, którego składający nie widzi, daje 404, a zgłoszenie
     * nieprzyjęte 409 z tym zgłoszeniem. Ponowne wysłanie zapisanego już
     * zgłoszenia nie jest sprawdzane, bo serwer zwraca zapisane.
     *
     * <p>Wymagania: BR-3, BR-5, BR-18, BR-9</p>
     *
     * @param finder Odczyt zgłoszeń
     * @param reader Osoba, która składa zmianę
     */
    void checkReplaced(final SubmissionFinder finder, final Reader reader) {
        final @Nullable UUID replaced = this.content.replaces();
        final @Nullable UUID saved = this.id;
        if (replaced != null && (saved == null || finder.find(saved).isEmpty())) {
            final Submission current = finder.visible(replaced, reader)
                .orElseThrow(() -> new MissingResource("Nie ma zastępowanego zgłoszenia"));
            if (!current.accepted()) {
                throw new ConflictingState(
                    "Zmienić można tylko zgłoszenie przyjęte; nieprzyjęte edytuje autor",
                    "serverSubmission",
                    new SubmissionResponse(current)
                );
            }
        }
    }
}
