/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.content;

import org.jspecify.annotations.Nullable;

/**
 * Piętro zgłoszenia, schemat SubmissionFloor w OpenAPI.
 *
 * <p>Wymagania: BR-G1</p>
 *
 * @param floorId Identyfikator piętra albo null, gdy nie podano
 * @param number Numer piętra albo null, gdy nie podano
 * @param mezzanine Czy to półpiętro; brak znaczy fałsz
 * @since 0.1.0
 */
public record SubmissionFloor(
    @Nullable String floorId, @Nullable Integer number, boolean mezzanine
) {

    /**
     * Czy piętro da się wskazać identyfikatorem albo numerem.
     *
     * @return Prawda, gdy podano identyfikator albo numer
     */
    public boolean identified() {
        return this.floorId != null || this.number != null;
    }
}
