/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.dostepnykampus.barrier.domain.BarrierDetails;
import io.github.dostepnykampus.barrier.domain.BarrierType;
import io.github.dostepnykampus.barrier.domain.InvalidSubmission;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Szczegóły bariery w JSON: pola SubmissionContent związane z barierą.
 *
 * @param barrierGroup Grupa barier albo null
 * @param barrierType Typ bariery TB-n albo null
 * @param severity Uciążliwość albo null
 * @param description Opis albo null
 * @param barrierRemoved Bariera usunięta albo null, gdy nie podano lub fałsz
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record BarrierJson(
    @Nullable GroupJson barrierGroup, @Nullable String barrierType,
    @Nullable Integer severity, @Nullable String description,
    @Nullable Boolean barrierRemoved
) {

    /**
     * Zapisuje szczegóły bariery; fałszywe usunięcie nie trafia do JSON.
     *
     * @param details Szczegóły bariery
     */
    BarrierJson(final BarrierDetails details) {
        this(
            Optional.ofNullable(details.group()).map(GroupJson::of).orElse(null),
            Optional.ofNullable(details.type()).map(BarrierType::code).orElse(null),
            details.severity(), details.description(),
            Optional.of(details.removed()).filter(Boolean::booleanValue).orElse(null)
        );
    }

    /**
     * Przenosi szczegóły z JSON do domeny.
     *
     * @return Szczegóły bariery
     * @throws InvalidSubmission Gdy typ albo uciążliwość są poza zakresem
     */
    BarrierDetails toDetails() throws InvalidSubmission {
        return new BarrierDetails(
            Optional.ofNullable(this.barrierGroup).map(GroupJson::group).orElse(null),
            Optional.ofNullable(this.barrierType).map(BarrierType::new).orElse(null),
            this.severity, this.description, Boolean.TRUE.equals(this.barrierRemoved)
        );
    }
}
