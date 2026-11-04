/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Odczyt zgłoszeń dla kontrolerów (US8).
 *
 * @since 0.1.0
 */
public interface SubmissionFinder {

    /**
     * Szuka zgłoszenia w dowolnym statusie.
     *
     * @param id Identyfikator zgłoszenia
     * @return Zgłoszenie albo puste, gdy go nie ma
     */
    Optional<Submission> find(UUID id);

    /**
     * Wyszukuje zgłoszenia według zapytania, od najstarszego, przy równej
     * chwili według identyfikatora.
     *
     * @param query Zapytanie
     * @return Zgłoszenia pasujące do zapytania
     */
    List<Submission> list(SubmissionQuery query);
}
