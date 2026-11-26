/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity

/** Wejścia „Wejście B” i „Wejście C” budynku C-13, oba „dostępne”, z drzwiami 100 cm. Wymagania: US2.1 */
internal val TWO_ENTRANCES: List<Entrance> = listOf(sampleEntrance("b"), sampleEntrance("c"))

/** Wejście „dostępne” [id] budynku C-13 z drzwiami 100 cm. Wymagania: US2.1 */
internal fun sampleEntrance(id: String): Entrance =
    Entrance(
        id = id,
        buildingSymbol = "C-13",
        name = "Wejście ${id.uppercase()}",
        isMain = false,
        type = "yes",
        location = GeoPoint(longitude = 0.5, latitude = 0.5),
        marking = EntranceMarking.ACCESSIBLE,
        doorWidthCm = 100,
    )

/** Przyjęta bariera [type] o uciążliwości [severity] przy wejściu [entrance] budynku C-13. Wymagania: US2.2 */
internal fun entranceBarrier(
    entrance: String,
    severity: Severity,
    type: BarrierType = BarrierType.HEAVY_DOOR,
): AcceptedBarrier =
    AcceptedBarrier(
        id = "$entrance-${severity.value}",
        location = GeoPoint(longitude = 0.5, latitude = 0.5),
        severity = severity,
        type = type,
        buildingSymbol = "C-13",
        verifiedAt = null,
        entranceId = entrance,
    )
