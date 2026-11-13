/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.reports

import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind

/**
 * Buduje treść zgłoszenia szybkiego; poza konturem pomija piętro, a budynek pozostawia do ustalenia serwerowi.
 *
 * Wymagania: BR-14, US4.6
 *
 * @param group grupa barier
 * @param photoIds identyfikatory zdjęć w telefonie
 * @param position pozycja z dokładnością i informacją o konturze
 * @param floor piętro; identyfikator z planu ma pierwszeństwo przed numerem, `null` oznacza brak wyboru
 * @param entranceId wejście, przy którym zgłoszono barierę; `null`, gdy nie podano
 */
fun quickSubmissionContent(
    group: BarrierGroup,
    photoIds: List<String>,
    position: QuickReportLocation,
    floor: SubmissionFloor? = null,
    entranceId: String? = null,
): SubmissionContent =
    SubmissionContent(
        kind = SubmissionKind.QUICK,
        location = position.point,
        photoIds = photoIds,
        accuracyMeters = position.accuracyMeters,
        floor = quickFloor(floor).takeIf { position.isInBuilding },
        barrierGroup = group,
        entranceId = entranceId,
    )

private fun quickFloor(chosen: SubmissionFloor?): SubmissionFloor? =
    when {
        chosen?.floorId != null -> SubmissionFloor(floorId = chosen.floorId)
        chosen?.number != null -> chosen
        else -> null
    }
