/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.removed

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionFloor

/** Przyjęte zgłoszenie zepsutej windy w C-13 na piętrze 2, którego dotyczą testy C7. */
internal val acceptedLift: AcceptedBarrier =
    AcceptedBarrier(
        id = "tb9",
        location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
        severity = Severity.BLOCKING,
        type = BarrierType.BROKEN_LIFT,
        buildingSymbol = "C-13",
        verifiedAt = null,
        floor = SubmissionFloor(number = 2, isMezzanine = false),
    )
