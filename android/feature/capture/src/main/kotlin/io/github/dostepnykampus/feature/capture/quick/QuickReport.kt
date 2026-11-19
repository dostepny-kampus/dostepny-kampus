/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.core.model.submission.SubmissionKind
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
 * Miejsce zgłoszenia szybkiego: bieżąca pozycja albo punkt wejścia wybranego w arkuszu wejścia.
 *
 * Wymagania: US4.1, NFR-27, BR-G2, BR-8, US4.6
 *
 * @property point miejsce, w którym stoi Odwiedzający, albo punkt wejścia
 * @property accuracyMeters dokładność pozycji w metrach; `null` przy punkcie wejścia
 * @property isInBuilding czy pozycja leży w konturze budynku; przy wejściu prawda
 * @property buildingSymbol budynek, w którego konturze leży pozycja, albo budynek wejścia
 * @property entrance wejście, od którego pochodzi miejsce; `null` przy bieżącej pozycji
 */
data class QuickPosition(
    val point: GeoPoint,
    val accuracyMeters: Double?,
    val isInBuilding: Boolean,
    val buildingSymbol: String? = null,
    val entrance: Entrance? = null,
)

/**
 * Miejsce zgłoszenia w punkcie wejścia [entrance], z jego budynkiem.
 *
 * Wymagania: BR-8, US4.6
 */
fun entrancePosition(entrance: Entrance): QuickPosition =
    QuickPosition(
        point = entrance.location,
        accuracyMeters = null,
        isInBuilding = true,
        buildingSymbol = entrance.buildingSymbol,
        entrance = entrance,
    )

/**
 * Czy krok 2 pyta o piętro po wyborze grupy: w konturze budynku i przy każdej grupie poza „Coś innego”.
 * Gdy pozycja jeszcze nie jest znana ([isInBuilding] `null`), pyta jak w konturze.
 *
 * Wymagania: BR-14
 */
fun BarrierGroup.asksFloor(isInBuilding: Boolean?): Boolean = this != BarrierGroup.OTHER && isInBuilding != false

/**
 * Treść zgłoszenia szybkiego z miejscem [position]; budynek ustala serwer. Poza konturem zgłoszenie idzie bez
 * piętra, także gdy Odwiedzający je wybrał, zanim telefon ustalił pozycję. Przy wejściu wybranym w arkuszu wejścia
 * zgłoszenie ma punkt i piętro wejścia oraz jego identyfikator.
 *
 * Wymagania: BR-14, US4.6, BR-8
 */
fun QuickReport.content(position: QuickPosition): SubmissionContent =
    SubmissionContent(
        kind = SubmissionKind.QUICK,
        location = position.point,
        photoIds = listOf(photo.photoId),
        accuracyMeters = position.accuracyMeters,
        floor = position.entranceFloor() ?: floor().takeIf { position.isInBuilding },
        barrierGroup = group,
        entranceId = position.entrance?.id ?: entranceId,
    )

private fun QuickPosition.entranceFloor(): SubmissionFloor? = entrance?.floorId?.let { SubmissionFloor(floorId = it) }

private fun QuickReport.floor(): SubmissionFloor? =
    when {
        floorId != null -> SubmissionFloor(floorId = floorId)
        floorNumber != null -> SubmissionFloor(number = floorNumber)
        else -> null
    }

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
