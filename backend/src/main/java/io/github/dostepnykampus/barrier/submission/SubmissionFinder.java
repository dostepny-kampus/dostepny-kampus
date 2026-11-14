/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port odczytu zgłoszeń; z niego korzystają kontrolery.
 *
 * <p>Wymagania: US8</p>
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
     * Szuka zgłoszenia, które czytelnik może zobaczyć: przyjęte widzi każdy,
     * pozostałe tylko autor i Weryfikator.
     *
     * <p>Wymagania: BR-5, BR-6</p>
     *
     * @param id Identyfikator zgłoszenia
     * @param reader Osoba, która czyta zgłoszenie
     * @return Zgłoszenie albo puste, gdy go nie ma albo czytelnik go nie widzi
     */
    Optional<Submission> visible(UUID id, Reader reader);

    /**
     * Sprawdza, czy zgłoszenie istnieje i złożyło je inne konto.
     * Zgłoszenie bez zapisanego autora też jest cudze.
     *
     * <p>Wymagania: BR-3</p>
     *
     * @param id Identyfikator zgłoszenia
     * @param account Identyfikator konta, które chce zmienić zgłoszenie
     * @return Prawda dla cudzego zgłoszenia; fałsz dla własnego i dla braku zgłoszenia
     */
    boolean foreign(UUID id, String account);

    /**
     * Sprawdza, czy zwykłą zmianę zgłoszenia przyjętego może złożyć tylko
     * autor, a konto nim nie jest: zgłoszenie istnieje, złożyło je
     * inne konto i nie jest zgłoszeniem szybkim.
     *
     * <p>Wymagania: BR-18</p>
     *
     * @param id Identyfikator zastępowanego zgłoszenia
     * @param account Identyfikator konta, które składa zmianę
     * @return Prawda dla cudzego zgłoszenia innego niż szybkie; fałsz dla
     *  własnego, dla szybkiego i dla braku zgłoszenia
     */
    boolean authorOnly(UUID id, String account);

    /**
     * Wyszukuje zgłoszenia według zapytania.
     *
     * @param query Statusy oraz opcjonalny budynek i piętro
     * @return Zgłoszenia posortowane po chwili utworzenia, potem po identyfikatorze
     */
    List<Submission> list(SubmissionQuery query);

    /**
     * Wyszukuje zgłoszenia jednego autora według zapytania.
     *
     * <p>Wymagania: US8.1</p>
     *
     * @param author Identyfikator konta autora
     * @param query Statusy oraz opcjonalny budynek i piętro
     * @return Zgłoszenia autora posortowane po chwili utworzenia, potem po identyfikatorze
     */
    List<Submission> own(String author, SubmissionQuery query);

    /**
     * Wyszukuje zgłoszenia podobne do danego, bez niego samego.
     *
     * <p>Wymagania: US10.1, BR-13</p>
     *
     * @param id Identyfikator sprawdzanego zgłoszenia
     * @return Podobne zgłoszenia od najbliższego; puste, gdy zgłoszenia nie ma
     */
    List<SimilarSubmission> similar(UUID id);
}
