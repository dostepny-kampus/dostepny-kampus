/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.reports

import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind

/**
 * Dane zgłoszenia szybkiego z jednym zdjęciem i opcjonalnym piętrem.
 *
 * Wymagania: US4.1, US4.3, US4.5, BR-14, US4.6
 *
 * @property photo zdjęcie do zapisania
 * @property group grupa barier
 * @property floorNumber piętro; `null` to „Nie wiem” albo brak pytania
 * @property floorId piętro z danych PWr w budynku szczegółowym zamiast [floorNumber]
 * @property floorLabel etykieta piętra [floorId] do prezentacji
 * @property entranceId wejście z „Zgłoś barierę przy wejściu”; `null`, gdy zgłoszenie zaczęło się z mapy
 */
public data class QuickReport(
    public val photo: PendingPhotoDraft,
    public val group: BarrierGroup,
    public val floorNumber: Int? = null,
    public val floorId: String? = null,
    public val floorLabel: String? = null,
    public val entranceId: String? = null,
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
public data class QuickPosition(
    public val point: GeoPoint,
    public val accuracyMeters: Double,
    public val isInBuilding: Boolean,
    public val buildingSymbol: String? = null,
)

/**
 * Treść zgłoszenia szybkiego z bieżącą pozycją; budynek ustala serwer z konturu. Poza konturem
 * zgłoszenie idzie bez piętra, także gdy Odwiedzający je wybrał, zanim telefon ustalił pozycję. Wejście wybrane
 * przez osobę zgłaszającą idzie razem z miejscem z pozycji.
 *
 * Wymagania: BR-14, US4.6
 */
public fun QuickReport.content(position: QuickPosition): SubmissionContent =
    SubmissionContent(
        kind = SubmissionKind.QUICK,
        location = position.point,
        photoIds = listOf(photo.photoId),
        accuracyMeters = position.accuracyMeters,
        floor = floor().takeIf { position.isInBuilding },
        barrierGroup = group,
        entranceId = entranceId,
    )

private fun QuickReport.floor(): SubmissionFloor? =
    when {
        floorId != null -> SubmissionFloor(floorId = floorId)
        floorNumber != null -> SubmissionFloor(number = floorNumber)
        else -> null
    }
