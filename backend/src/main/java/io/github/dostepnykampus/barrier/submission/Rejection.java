/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Odrzucenie zgłoszenia przez Weryfikatora: powód widoczny dla autora i, przy duplikacie,
 * wcześniejsze zgłoszenie.
 *
 * <p>Wymagania: BR-13, BR-16</p>
 *
 * @param reason Niepusty powód odrzucenia
 * @param duplicateOf Wcześniejsze zgłoszenie wskazane przy duplikacie albo {@code null}
 * @since 0.1.0
 */
public record Rejection(String reason, @Nullable UUID duplicateOf) {

    /**
     * Odrzucenie z powodem; brak powodu kończy się wyjątkiem NullPointerException.
     */
    public Rejection {
        Objects.requireNonNull(reason, "reason");
    }
}
