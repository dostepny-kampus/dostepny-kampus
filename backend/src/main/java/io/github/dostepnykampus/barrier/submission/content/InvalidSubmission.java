/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.content;

import io.github.dostepnykampus.shared.error.InvalidInput;

/**
 * Dane zgłoszenia naruszają jego inwariant; opis nie zawiera sekretów.
 *
 * <p>Jako {@link InvalidInput} w obsłudze żądania daje odpowiedź 400
 * z opisem w {@code detail}.</p>
 *
 * @since 0.1.0
 */
public final class InvalidSubmission extends InvalidInput {

    /**
     * Identyfikator postaci serializowanej wyjątku.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Tworzy błąd niepoprawnego zgłoszenia.
     *
     * @param message Bezpieczny opis naruszonego inwariantu
     */
    public InvalidSubmission(final String message) {
        super(message);
    }
}
