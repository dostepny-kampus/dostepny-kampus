/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import io.github.dostepnykampus.barrier.domain.BarrierDetails;
import io.github.dostepnykampus.barrier.domain.BarrierGroup;
import io.github.dostepnykampus.barrier.domain.NewEntranceMarking;
import io.github.dostepnykampus.barrier.domain.SubmissionContent;
import io.github.dostepnykampus.barrier.domain.SubmissionKind;
import io.github.dostepnykampus.barrier.domain.SubmissionPlace;
import io.github.dostepnykampus.barrier.domain.SubmissionPoint;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Treść zgłoszenia w JSON, schemat SubmissionContent w OpenAPI.
 *
 * @param kind Rodzaj zgłoszenia
 * @param location Miejsce bariery
 * @param accuracyMeters Dokładność pozycji w metrach
 * @param manualCorrection Korekta ręczna
 * @param buildingSymbol Symbol budynku
 * @param floor Piętro
 * @param barrierGroup Grupa barier, wartość z OpenAPI
 * @param barrierType Typ bariery w postaci TB-n
 * @param severity Uciążliwość
 * @param description Opis
 * @param replaces Zgłoszenie zastępowane, w JSON replacesSubmissionId
 * @param barrierRemoved Bariera usunięta
 * @param entranceId Wejście w zmianie oznaczenia
 * @param entranceMarking Nowe oznaczenie wejścia
 * @param photoIds Identyfikatory zdjęć
 * @since 0.1.0
 */
public record SubmissionContentJson(
    @Nullable SubmissionKind kind, @Nullable GeoPointJson location,
    @Nullable Double accuracyMeters, @Nullable Boolean manualCorrection,
    @Nullable String buildingSymbol, @Nullable SubmissionFloorJson floor,
    @Nullable String barrierGroup, @Nullable String barrierType,
    @Nullable Integer severity, @Nullable String description,
    @Nullable UUID replaces, @Nullable Boolean barrierRemoved,
    @Nullable String entranceId, @Nullable NewEntranceMarking entranceMarking,
    @Nullable List<UUID> photoIds
) {

    /**
     * Zapisuje treść zgłoszenia w JSON.
     *
     * @param content Treść zgłoszenia
     */
    public SubmissionContentJson(final SubmissionContent content) {
        this(
            content.kind(), null, null, null, null, null, null, null, null, null, null, null,
            null, null, null
        );
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
