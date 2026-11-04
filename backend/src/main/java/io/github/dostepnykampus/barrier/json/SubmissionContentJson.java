/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import io.github.dostepnykampus.barrier.domain.BarrierDetails;
import io.github.dostepnykampus.barrier.domain.BarrierGroup;
import io.github.dostepnykampus.barrier.domain.SubmissionContent;
import io.github.dostepnykampus.barrier.domain.SubmissionKind;
import io.github.dostepnykampus.barrier.domain.SubmissionPlace;
import io.github.dostepnykampus.barrier.domain.SubmissionPoint;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Treść zgłoszenia w JSON, schemat SubmissionContent w OpenAPI. Zaślepka
 * z jednym polem; pozostałe pola trzeba rozbić na zagnieżdżone rekordy
 * ({@code @JsonUnwrapped}), bo qulice dopuszcza do ośmiu komponentów rekordu.
 *
 * @param kind Rodzaj zgłoszenia
 * @param location Miejsce bariery
 * @param floor Piętro
 * @since 0.1.0
 */
public record SubmissionContentJson(
    @Nullable SubmissionKind kind, @Nullable GeoPointJson location,
    @Nullable SubmissionFloorJson floor
) {

    /**
     * Zapisuje treść zgłoszenia w JSON.
     *
     * @param content Treść zgłoszenia
     */
    public SubmissionContentJson(final SubmissionContent content) {
        this(content.kind(), null, null);
    }

    /**
     * Przenosi treść z JSON do domeny.
     *
     * @return Treść zgłoszenia
     */
    public SubmissionContent toContent() {
        return new SubmissionContent(
            SubmissionKind.QUICK,
            new SubmissionPlace(new SubmissionPoint(0.0, 0.0), null, false, null, null),
            new BarrierDetails(BarrierGroup.OTHER, null, null, null, false),
            null, null, null, List.of(new UUID(0L, 0L))
        );
    }
}
