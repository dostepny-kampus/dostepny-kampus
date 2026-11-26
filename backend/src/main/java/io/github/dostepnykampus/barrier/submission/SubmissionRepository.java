/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import java.util.Optional;
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
     * Zmienia status zgłoszenia przyjętego na archiwalny. Zgłoszenie w innym statusie
     * albo nieistniejące zostaje bez zmiany.
     *
     * <p>Wymagania: BR-18, BR-19, BR-24</p>
     *
     * @param id Identyfikator zgłoszenia
     * @return Zgłoszenie archiwalne albo brak, gdy zgłoszenie nie było przyjęte
     */
    Optional<Submission> archive(UUID id);
}
