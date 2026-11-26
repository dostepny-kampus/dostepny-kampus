/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.persistence;

import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Przejścia statusu przy edycji zgłoszenia jako pary tablic parametrów SQL: status
 * edytowalny i status po edycji pod tym samym indeksem.
 *
 * <p>Wymagania: BR-9, BR-10</p>
 *
 * @since 0.1.0
 */
final class EditTransition {

    private EditTransition() {
    }

    /**
     * Nazwy statusów, które wolno edytować.
     *
     * @return Nazwy statusów w kolejności wyliczenia
     */
    static String[] from() {
        return EditTransition.names(Function.identity());
    }

    /**
     * Nazwy statusów po edycji, pod tym samym indeksem co w {@link #from()}.
     *
     * @return Nazwy statusów po edycji
     */
    static String[] to() {
        return EditTransition.names(SubmissionStatus::afterEdit);
    }

    private static String[] names(final Function<SubmissionStatus, SubmissionStatus> after) {
        return Arrays.stream(SubmissionStatus.values())
            .filter(SubmissionStatus::editable)
            .map(after)
            .map(Enum::name)
            .toArray(String[]::new);
    }
}
