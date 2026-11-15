/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import java.util.UUID;

/**
 * Port zapisu zgłoszeń; z niego korzysta wyłącznie usługa zgłoszeń.
 *
 * <p>Wymagania: BR-3, BR-9, BR-10</p>
 *
 * @since 0.1.0
 */
public interface SubmissionRepository {

    /**
     * Blokuje istniejące zgłoszenie przed zmianą do końca bieżącej transakcji.
     * Brak zgłoszenia nie jest błędem; wywołujący sprawdza go przy odczycie.
     *
     * @param id Identyfikator zgłoszenia
     */
    void lock(UUID id);

    /**
     * Zapisuje nowe zgłoszenie albo zwraca autorowi już zapisane o tym identyfikatorze;
     * identyfikator zgłoszenia innego autora kończy się wyjątkiem ForbiddenChange bez zapisu.
     *
     * @param submission Komenda utworzenia
     * @return Zgłoszenie utworzone albo istniejące, bez zmiany treści i wersji
     */
    CreateResult create(NewSubmission submission);

    /**
     * Zapisuje edycję, gdy wersja bazowa zgadza się z zapisaną i status pozwala edytować.
     * Edycja z innym zastępowanym zgłoszeniem niż zapisane kończy się wyjątkiem
     * InvalidSubmission bez zapisu.
     *
     * @param edit Komenda edycji
     * @return Zgłoszenie zapisane, bieżący stan przy konflikcie albo brak zgłoszenia
     */
    EditResult update(SubmissionEdit edit);

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
}
