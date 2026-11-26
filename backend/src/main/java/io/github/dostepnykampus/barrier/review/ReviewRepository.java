/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.review;

import io.github.dostepnykampus.barrier.submission.EditResult;
import io.github.dostepnykampus.barrier.submission.Submission;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Port weryfikacji zgłoszeń: wzięcie do sprawdzenia, decyzja, kolejka i podobne zgłoszenia.
 *
 * <p>Wymagania: US10, US10.1, US10.4, BR-11, BR-13</p>
 *
 * @since 0.1.0
 */
public interface ReviewRepository {

    /**
     * Daje zgłoszenie „wysłane” Weryfikatorowi, a wzięte już przez niego zostawia bez zmiany.
     *
     * <p>Wymagania: BR-11, US10.4</p>
     *
     * @param id Identyfikator zgłoszenia
     * @param reviewer Identyfikator konta Weryfikatora
     * @return Zgłoszenie w weryfikacji, bieżący stan przy konflikcie albo brak zgłoszenia
     */
    EditResult claim(UUID id, String reviewer);

    /**
     * Zapisuje decyzję, gdy zgłoszenie jest w weryfikacji u tego Weryfikatora
     * i ma oczekiwaną wersję.
     *
     * <p>Wymagania: BR-3, BR-11, BR-16</p>
     *
     * @param decision Decyzja Weryfikatora
     * @return Zgłoszenie po decyzji, bieżący stan przy konflikcie albo brak zgłoszenia
     */
    EditResult decide(ReviewDecision decision);

    /**
     * Sprawdza, czy zgłoszenie istnieje i wzięło je do weryfikacji to konto.
     *
     * <p>Wymagania: BR-11</p>
     *
     * @param id Identyfikator zgłoszenia
     * @param account Identyfikator konta Weryfikatora
     * @return Prawda, gdy konto wzięło zgłoszenie; fałsz dla cudzego, niczyjego i braku
     */
    boolean claimedBy(UUID id, String account);

    /**
     * Wyszukuje zgłoszenia podobne do danego, bez niego samego.
     *
     * <p>Wymagania: US10.1, BR-13</p>
     *
     * @param id Identyfikator sprawdzanego zgłoszenia
     * @return Podobne zgłoszenia od najbliższego; puste, gdy zgłoszenia nie ma
     */
    List<SimilarSubmission> similar(UUID id);

    /**
     * Wyszukuje zgłoszenia w kolejce weryfikacji dla danego konta: „wysłane” innych osób
     * oraz „w weryfikacji” wzięte przez to konto, od najdawniej wysłanego.
     *
     * <p>Wymagania: US10.4, US10.5, US10.7, BR-4, BR-11</p>
     *
     * @param reviewer Identyfikator konta Weryfikatora
     * @return Zgłoszenia w kolejce posortowane po chwili utworzenia, potem po identyfikatorze
     */
    List<Submission> reviewQueue(String reviewer);

    /**
     * Odczytuje autorów zgłoszeń.
     *
     * <p>Wymagania: BR-5</p>
     *
     * @param ids Identyfikatory zgłoszeń
     * @return Autor według identyfikatora zgłoszenia; bez zgłoszeń, których autora serwer nie zna
     */
    Map<UUID, SubmissionAuthor> authors(Collection<UUID> ids);
}
