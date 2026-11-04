/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.UUID;

/**
 * Wynik edycji zgłoszenia (BR-3, BR-10, BR-18).
 *
 * @since 0.1.0
 */
public sealed interface EditResult
    permits EditResult.Saved, EditResult.Conflict, EditResult.Missing {

    /**
     * Edycja zapisana.
     *
     * @param submission Zgłoszenie po zapisie
     * @since 0.1.0
     */
    record Saved(Submission submission) implements EditResult {
    }

    /**
     * Zła wersja bazowa albo status, którego nie wolno edytować.
     *
     * @param current Bieżący stan zgłoszenia na serwerze
     * @since 0.1.0
     */
    record Conflict(Submission current) implements EditResult {
    }

    /**
     * Nie ma zgłoszenia o tym identyfikatorze.
     *
     * @param id Identyfikator szukanego zgłoszenia
     * @since 0.1.0
     */
    record Missing(UUID id) implements EditResult {
    }
}
