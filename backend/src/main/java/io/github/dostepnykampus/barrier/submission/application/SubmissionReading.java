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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    /**
     * Ctor.
     *
     * @param finder Port odczytu zgłoszeń
     */
    public SubmissionReading(final SubmissionFinder finder) {
        this.finder = finder;
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
     * Wyszukuje zgłoszenia jednego autora.
     *
     * <p>Wymagania: US8.1</p>
     *
     * @param author Identyfikator konta autora
     * @param query Statusy oraz opcjonalny budynek i piętro
     * @return Zgłoszenia autora
     */
    public List<Submission> own(final String author, final SubmissionQuery query) {
        return this.finder.own(author, query);
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
