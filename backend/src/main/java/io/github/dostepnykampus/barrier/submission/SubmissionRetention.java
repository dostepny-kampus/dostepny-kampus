/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import java.util.List;
import java.util.UUID;

/**
 * Port usuwania zgłoszeń odrzuconych po upływie okresu przechowywania.
 *
 * <p>Wymagania: NFR-29</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface SubmissionRetention {

    /**
     * Usuwa zgłoszenia, które od odrzucenia mają w statusie odrzuconym więcej dni niż podano.
     *
     * @param days Okres przechowywania w dniach
     * @return Identyfikatory usuniętych zgłoszeń
     */
    List<UUID> purgeRejected(int days);
}
