/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.application;

import io.github.dostepnykampus.barrier.submission.Reader;
import io.github.dostepnykampus.barrier.submission.SimilarSubmission;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import io.github.dostepnykampus.barrier.submission.SubmissionQuery;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import io.github.dostepnykampus.building.api.KnownBuildings;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Odczyt zgłoszeń dla warstwy web: kontroler nie sięga do portu
 * {@link SubmissionFinder}, tylko do tej usługi.
 *
 * <p>Wymagania: US8</p>
 *
 * @since 0.1.0
 */
@Service
public final class SubmissionReading {

    /** Port odczytu zgłoszeń. */
    private final SubmissionFinder finder;

    /** Etykiety pięter. */
    private final KnownBuildings floors;

    /**
     * Ctor.
     *
     * @param finder Port odczytu zgłoszeń
     * @param floors Etykiety pięter
     */
    public SubmissionReading(final SubmissionFinder finder, final KnownBuildings floors) {
        this.finder = finder;
        this.floors = floors;
    }

    /**
     * Szuka zgłoszenia w dowolnym statusie.
     *
     * @param id Identyfikator zgłoszenia
     * @return Zgłoszenie albo puste
     */
    public Optional<Submission> find(final UUID id) {
        return this.finder.find(id);
    }

    /**
     * Szuka zgłoszenia, które czytelnik może zobaczyć.
     *
     * <p>Wymagania: BR-5, BR-6</p>
     *
     * @param id Identyfikator zgłoszenia
     * @param reader Osoba, która czyta zgłoszenie
     * @return Zgłoszenie albo puste
     */
    public Optional<Submission> visible(final UUID id, final Reader reader) {
        return this.finder.visible(id, reader);
    }

    /**
     * Sprawdza, czy zgłoszenie istnieje i złożyło je inne konto.
     *
     * <p>Wymagania: BR-3</p>
     *
     * @param id Identyfikator zgłoszenia
     * @param account Identyfikator konta
     * @return Prawda dla cudzego zgłoszenia
     */
    public boolean foreign(final UUID id, final String account) {
        return this.finder.foreign(id, account);
    }

    /**
     * Sprawdza, czy zgłoszenie wzięło do weryfikacji to konto.
     *
     * <p>Wymagania: BR-11</p>
     *
     * @param id Identyfikator zgłoszenia
     * @param account Identyfikator konta Weryfikatora
     * @return Prawda, gdy konto wzięło zgłoszenie
     */
    public boolean claimedBy(final UUID id, final String account) {
        return this.finder.claimedBy(id, account);
    }

    /**
     * Sprawdza, czy zmianę przyjętego zgłoszenia może złożyć tylko autor, a konto nim nie jest.
     *
     * <p>Wymagania: BR-18</p>
     *
     * @param id Identyfikator zastępowanego zgłoszenia
     * @param account Identyfikator konta
     * @return Prawda dla cudzego zgłoszenia innego niż szybkie
     */
    public boolean authorOnly(final UUID id, final String account) {
        return this.finder.authorOnly(id, account);
    }

    /**
     * Przyjęte zgłoszenia istniejących barier, czytane stronami po 500 w miarę odbioru.
     *
     * <p>Wymagania: US13.2, NFR-34</p>
     *
     * @return Leniwy strumień zgłoszeń w kolejności identyfikatorów
     */
    public Stream<Submission> exportable() {
        final SubmissionQuery accepted = new SubmissionQuery(
            Set.of(SubmissionStatus.ACCEPTED), null, null
        );
        return new SubmissionPages(after -> this.finder.page(accepted, after, 500)).stream()
            .filter(item -> !item.content().barrier().removed())
            .filter(item -> item.content().kind() != SubmissionKind.ENTRANCE_MARKING);
    }

    /**
     * Szuka etykiety piętra na planie.
     *
     * @param symbol Budynek albo null
     * @param id Piętro albo null
     * @return Etykieta albo puste, gdy brak danych
     */
    public Optional<String> floorLabel(final @Nullable String symbol, final @Nullable String id) {
        return this.floors.floorLabel(symbol, id);
    }

    /**
     * Wyszukuje stronę zgłoszeń.
     *
     * <p>Wymagania: NFR-34, US1.3</p>
     *
     * @param query Statusy oraz opcjonalny budynek i piętro
     * @param after Kursor: strona zaczyna się po nim
     * @param limit Najwięcej zgłoszeń na stronie
     * @return Czytelne zgłoszenia strony i kursor następnej
     */
    public SubmissionFinder.Page page(
        final SubmissionQuery query, final UUID after, final int limit
    ) {
        return this.finder.page(query, after, limit);
    }

    /**
     * Wyszukuje stronę zgłoszeń jednego autora.
     *
     * <p>Wymagania: NFR-34, US8.1</p>
     *
     * @param author Identyfikator konta autora
     * @param query Statusy oraz opcjonalny budynek i piętro
     * @param after Kursor: strona zaczyna się po nim
     * @param limit Najwięcej zgłoszeń na stronie
     * @return Czytelne zgłoszenia strony autora i kursor następnej
     */
    public SubmissionFinder.Page own(
        final String author, final SubmissionQuery query, final UUID after, final int limit
    ) {
        return this.finder.own(author, query, after, limit);
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
        return this.finder.similar(id);
    }
}
