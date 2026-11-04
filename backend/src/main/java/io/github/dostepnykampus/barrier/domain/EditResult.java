/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.Objects;
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

        /**
         * Wynik z zapisanym zgłoszeniem; brak zgłoszenia kończy się wyjątkiem
         * NullPointerException.
         *
         * @param submission Zgłoszenie z wyniku
         */
        public Saved {
            Objects.requireNonNull(submission, "submission");
        }
    }

    /**
     * Zła wersja bazowa albo status, którego nie wolno edytować.
     *
     * @param current Bieżący stan zgłoszenia na serwerze
     * @since 0.1.0
     */
    record Conflict(Submission current) implements EditResult {

        /**
         * Wynik z bieżącym stanem; brak zgłoszenia kończy się wyjątkiem
         * NullPointerException.
         *
         * @param current Bieżące zgłoszenie
         */
        public Conflict {
            Objects.requireNonNull(current, "current");
        }
    }

    /**
     * Nie ma zgłoszenia o tym identyfikatorze.
     *
     * @param id Identyfikator szukanego zgłoszenia
     * @since 0.1.0
     */
    record Missing(UUID id) implements EditResult {

        /**
         * Wynik z identyfikatorem; brak identyfikatora kończy się wyjątkiem
         * NullPointerException.
         *
         * @param id Identyfikator szukanego zgłoszenia
         */
        public Missing {
            Objects.requireNonNull(id, "id");
        }
    }
}
