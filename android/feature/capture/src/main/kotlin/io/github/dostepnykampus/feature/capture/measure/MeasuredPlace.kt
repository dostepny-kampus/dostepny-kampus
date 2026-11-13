/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.measure

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.capture.ReportPlace
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute

/**
 * Trasa do C2 po „Dalej” na C1 albo `null`, gdy „Dalej” jeszcze nie działa: pomiar trwa albo budynek czeka na decyzję
 * Zbierającego. Budynek potwierdzony albo wybrany idzie z nazwą, „poza budynkiem” bez budynku.
 *
 * Wymagania: US6.2, BR-G4
 */
fun MeasureUiState.reportFormRoute(): ReportFormRoute? {
    val finished = this as? MeasureUiState.Finished ?: return null
    val point = finished.point
    val accuracy = finished.average.accuracyMeters
    val isCorrected = finished.corrected != null
    return when (val place = finished.place) {
        is PlaceChoice.Chosen -> {
            ReportFormRoute(
                longitude = point.longitude,
                latitude = point.latitude,
                accuracyMeters = accuracy,
                buildingSymbol = place.building.symbol,
                buildingName = place.building.name,
                isManualCorrection = isCorrected,
            )
        }

        PlaceChoice.Outside -> {
            ReportFormRoute(
                longitude = point.longitude,
                latitude = point.latitude,
                accuracyMeters = accuracy,
                buildingSymbol = null,
                isManualCorrection = isCorrected,
            )
        }

        PlaceChoice.Locating, is PlaceChoice.Suggested -> {
            null
        }
    }
}

/** Miejsce na górze formularza C2 z trasy po pomiarze, z oznaczeniem korekty ręcznej. Wymagania: US6.3, US6.4 */
fun ReportFormRoute.place(): ReportPlace =
    ReportPlace(
        location = GeoPoint(longitude = longitude, latitude = latitude),
        accuracyMeters = accuracyMeters,
        buildingSymbol = buildingSymbol,
        isManualCorrection = isManualCorrection,
        buildingName = buildingName,
    )
