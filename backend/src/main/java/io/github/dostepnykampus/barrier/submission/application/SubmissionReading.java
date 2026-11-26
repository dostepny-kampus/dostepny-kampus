/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.application;

import io.github.dostepnykampus.barrier.submission.Reader;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionChange;
import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import io.github.dostepnykampus.barrier.submission.SubmissionQuery;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import io.github.dostepnykampus.building.api.KnownBuildings;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Odczyt zgłoszeń dla warstwy web: kontroler nie sięga do portu
 * {@link SubmissionFinder}, tylko do tej usługi.
 *
 * <p>Wymagania: US8</p>
 *
 * @since 0.1.0
 */
@Service
public class SubmissionReading {

    /** Port odczytu zgłoszeń. */
    private final SubmissionFinder finder;

    /** Port stron listy zgłoszeń. */
    private final SubmissionFinder.Pages pages;

    /** Etykiety pięter. */
    private final KnownBuildings floors;

    /**
     * Ctor.
     *
     * @param finder Port odczytu zgłoszeń
     * @param pages Port stron listy zgłoszeń
     * @param floors Etykiety pięter
     */
    public SubmissionReading(
        final SubmissionFinder finder, final SubmissionFinder.Pages pages,
        final KnownBuildings floors
    ) {
        this.finder = finder;
        this.pages = pages;
        this.floors = floors;
    }

    /**
     * Własne zgłoszenia konta w każdym statusie oraz cudze przyjęte i archiwalne, zmienione od
     * {@code since} włącznie, od najdawniej zmienionego, w jednej transakcji odczytu.
     *
     * <p>Wymagania: US8.1, US1.3, US10.6</p>
     *
     * @param since Chwila, od której liczą się zmiany
     * @param reader Identyfikator konta z tokenu
     * @return Zgłoszenia z chwilą zmiany
     */
    @Transactional(readOnly = true)
    public List<SubmissionChange> changedSince(final Instant since, final String reader) {
        return this.finder.changedSince(since, reader);
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
     * Wyszukuje zgłoszenia według zapytania.
     *
     * @param query Statusy oraz opcjonalny budynek i piętro
     * @return Zgłoszenia
     */
    public List<Submission> list(final SubmissionQuery query) {
        return this.finder.list(query);
    }

    /**
     * Strona przyjętych zgłoszeń istniejących barier. Pominięte zgłoszenia nadal
     * przesuwają kursor, nawet gdy strona nie ma wierszy do eksportu.
     *
     * @param after Kursor albo {@link SubmissionFinder.Page#START} dla pierwszej strony
     * @return Strona eksportu w kolejności identyfikatorów z kursorem listy zgłoszeń
     */
    public SubmissionFinder.Page exportablePage(final UUID after) {
        final SubmissionFinder.Page page = this.pages.page(
            new SubmissionQuery(Set.of(SubmissionStatus.ACCEPTED), null, null),
            new SubmissionFinder.Window(after, 500)
        );
        return new SubmissionFinder.Page(
            page.items().stream()
                .filter(item -> !item.content().barrier().removed())
                .filter(item -> item.content().kind() != SubmissionKind.ENTRANCE_MARKING)
                .toList(),
            page.last(), page.more()
        );
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
     * Wyszukuje stronę zgłoszeń, a z autorem tylko jego zgłoszenia.
     *
     * <p>Wymagania: NFR-34, US1.3, US8.1</p>
     *
     * @param query Statusy oraz opcjonalny budynek i piętro
     * @param author Identyfikator konta autora albo null dla zgłoszeń wszystkich kont
     * @param window Kursor i rozmiar strony
     * @return Czytelne zgłoszenia strony i kursor następnej
     */
    public SubmissionFinder.Page page(
        final SubmissionQuery query, final @Nullable String author,
        final SubmissionFinder.Window window
    ) {
        final SubmissionFinder.Page page;
        if (author == null) {
            page = this.pages.page(query, window);
        } else {
            page = this.pages.own(author, query, window);
        }
        return page;
    }
}
