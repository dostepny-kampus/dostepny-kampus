/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

/**
 * Zapis zgłoszeń, dostępny wyłącznie dla przypadków użycia (BR-3, BR-9, BR-10).
 *
 * @since 0.1.0
 */
public interface SubmissionRepository {

    /**
     * Zapisuje nowe zgłoszenie bez nadpisywania istniejącego (BR-9).
     *
     * @param submission Komenda utworzenia
     * @return Zgłoszenie utworzone tym wywołaniem albo już istniejące
     */
    CreateResult create(NewSubmission submission);

    /**
     * Zapisuje edycję, gdy wersja bazowa i status na to pozwalają (BR-10).
     *
     * @param edit Komenda edycji
     * @return Zapisane zgłoszenie, konflikt z bieżącym stanem albo brak zgłoszenia
     */
    EditResult update(SubmissionEdit edit);
}
