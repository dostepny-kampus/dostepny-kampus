/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.reports.QuickReport
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute

/**
 * Czy krok 2 pyta o piętro po wyborze grupy: w konturze budynku i przy każdej grupie poza „Coś innego”.
 * Gdy pozycja jeszcze nie jest znana ([isInBuilding] `null`), pyta jak w konturze.
 *
 * Wymagania: BR-14
 */
fun BarrierGroup.asksFloor(isInBuilding: Boolean?): Boolean = this != BarrierGroup.OTHER && isInBuilding != false

/** Zgłoszenie z danych kroków 1 i 2 albo `null`, gdy wpis historii ich nie ma. */
internal fun ReportRoute.quickReport(): QuickReport? {
    val id = photoId
    val path = photoPath
    val chosen = group
    return if (id != null && path != null && chosen != null) {
        QuickReport(
            photo = PendingPhotoDraft(photoId = id, path = path),
            group = chosen,
            floorNumber = floorNumber,
            floorId = floorId,
            floorLabel = floorLabel,
            entranceId = entranceId,
        )
    } else {
        null
    }
}
