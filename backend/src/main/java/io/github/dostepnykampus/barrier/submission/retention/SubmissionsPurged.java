/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.retention;

import java.util.List;
import java.util.UUID;

/**
 * Zdarzenie po usunięciu zgłoszeń odrzuconych; na nie usuwa się ich zdjęcia.
 *
 * <p>Wymagania: NFR-29</p>
 *
 * @param ids Identyfikatory usuniętych zgłoszeń
 * @since 0.1.0
 */
public record SubmissionsPurged(List<UUID> ids) {

    /**
     * Kopiuje listę identyfikatorów.
     */
    public SubmissionsPurged {
        ids = List.copyOf(ids);
    }
}
