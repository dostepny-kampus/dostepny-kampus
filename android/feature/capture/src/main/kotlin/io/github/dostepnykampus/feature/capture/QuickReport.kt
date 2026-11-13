/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.reports.QuickReportLocation
import io.github.dostepnykampus.core.data.reports.quickSubmissionContent
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute

/**
 * Zgłoszenie szybkie V4 po kroku 2: zdjęcie, grupa barier i odpowiedź na pytanie o piętro.
 *
 * Wymagania: US4.1, US4.3, US4.5, BR-14, US4.6
 *
 * @property photo zdjęcie z kroku 1
 * @property group grupa barier z kroku 2
 * @property floorNumber piętro z kroku 2; `null` to „Nie wiem” albo brak pytania
 * @property floorId piętro z danych PWr w budynku szczegółowym zamiast [floorNumber]
 * @property floorLabel etykieta piętra [floorId] do podsumowania w kroku 3
 * @property entranceId wejście z „Zgłoś barierę przy wejściu”; `null`, gdy zgłoszenie zaczęło się z mapy
 */
data class QuickReport(
    val photo: PendingPhotoDraft,
    val group: BarrierGroup,
    val floorNumber: Int? = null,
    val floorId: String? = null,
    val floorLabel: String? = null,
    val entranceId: String? = null,
)

/**
 * Bieżąca pozycja do zgłoszenia szybkiego.
 *
 * Wymagania: US4.1, NFR-27, BR-G2
 *
 * @property point miejsce, w którym stoi Odwiedzający
 * @property accuracyMeters dokładność pozycji w metrach
 * @property isInBuilding czy pozycja leży w konturze budynku
 * @property buildingSymbol budynek, w którego konturze leży pozycja
 */
data class QuickPosition(
    val point: GeoPoint,
    val accuracyMeters: Double,
    val isInBuilding: Boolean,
    val buildingSymbol: String? = null,
)

/**
 * Czy krok 2 pyta o piętro po wyborze grupy: w konturze budynku i przy każdej grupie poza „Coś innego”.
 * Gdy pozycja jeszcze nie jest znana ([isInBuilding] `null`), pyta jak w konturze.
 *
 * Wymagania: BR-14
 */
fun BarrierGroup.asksFloor(isInBuilding: Boolean?): Boolean = this != BarrierGroup.OTHER && isInBuilding != false

/**
 * Przekazuje dane zgłoszenia i bieżącą pozycję do wspólnej fabryki treści.
 *
 * Wymagania: BR-14, US4.6
 */
fun QuickReport.content(position: QuickPosition): SubmissionContent =
    quickSubmissionContent(
        group = group,
        photoIds = listOf(photo.photoId),
        position =
            QuickReportLocation(
                point = position.point,
                accuracyMeters = position.accuracyMeters,
                isInBuilding = position.isInBuilding,
            ),
        floorNumber = floorNumber,
        floorId = floorId,
        entranceId = entranceId,
    )

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
