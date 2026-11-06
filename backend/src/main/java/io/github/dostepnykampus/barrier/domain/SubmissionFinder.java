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
 * Port odczytu zgłoszeń; z niego korzystają kontrolery (US8).
 *
 * @since 0.1.0
 */
public interface SubmissionFinder {

    /**
     * Szuka zgłoszenia o danym identyfikatorze, w dowolnym statusie.
     *
     * @param id Identyfikator zgłoszenia
     * @return Zgłoszenie albo puste, gdy go nie ma
     */
    Optional<Submission> find(UUID id);

    /**
     * Wyszukuje zgłoszenia według zapytania.
     *
     * @param query Statusy oraz opcjonalny budynek i piętro
     * @return Zgłoszenia posortowane po chwili utworzenia, potem po identyfikatorze
     */
    List<Submission> list(SubmissionQuery query);

    /**
     * Wyszukuje zgłoszenia podobne do danego (US10.1, BR-13), bez niego samego.
     *
     * @param id Identyfikator sprawdzanego zgłoszenia
     * @return Podobne zgłoszenia od najbliższego; puste, gdy zgłoszenia nie ma
     */
    List<SimilarSubmission> similar(UUID id);
}
