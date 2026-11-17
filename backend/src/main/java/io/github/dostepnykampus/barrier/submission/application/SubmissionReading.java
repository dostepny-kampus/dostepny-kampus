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
     * Wyszukuje zgłoszenia według zapytania.
     *
     * @param query Statusy oraz opcjonalny budynek i piętro
     * @return Zgłoszenia
     */
    public List<Submission> list(final SubmissionQuery query) {
        return this.finder.list(query);
    }

    /**
     * Przyjęte zgłoszenia istniejących barier.
     *
     * @return Zgłoszenia w kolejności utworzenia
     */
    public List<Submission> exportable() {
        return this.finder.list(new SubmissionQuery(Set.of(SubmissionStatus.ACCEPTED), null, null))
            .stream().filter(item -> !item.content().barrier().removed())
            .filter(item -> item.content().kind() != SubmissionKind.ENTRANCE_MARKING)
            .toList();
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
