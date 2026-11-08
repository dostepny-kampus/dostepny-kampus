/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute

/**
 * Trasa do C2 po „Dalej” na C1 albo `null`, gdy „Dalej” jeszcze nie działa: pomiar trwa albo budynek czeka na decyzję
 * Zbierającego (US6.2). Budynek potwierdzony albo wybrany idzie z nazwą, „poza budynkiem” bez budynku (BR-G4).
 */
fun MeasureUiState.reportFormRoute(): ReportFormRoute? {
    val finished = this as? MeasureUiState.Finished ?: return null
    val point = finished.average.point
    val accuracy = finished.average.accuracyMeters
    return when (val place = finished.place) {
        is PlaceChoice.Chosen -> {
            ReportFormRoute(
                longitude = point.longitude,
                latitude = point.latitude,
                accuracyMeters = accuracy,
                buildingSymbol = place.building.symbol,
                buildingName = place.building.name,
            )
        }

        PlaceChoice.Outside -> {
            ReportFormRoute(
                longitude = point.longitude,
                latitude = point.latitude,
                accuracyMeters = accuracy,
                buildingSymbol = null,
            )
        }

        PlaceChoice.Locating, is PlaceChoice.Suggested -> {
            null
        }
    }
}

/** Miejsce na górze formularza C2 z trasy po pomiarze; punkt nie jest jeszcze poprawiany ręcznie (US6.3). */
fun ReportFormRoute.place(): ReportPlace =
    ReportPlace(
        location = GeoPoint(longitude = longitude, latitude = latitude),
        accuracyMeters = accuracyMeters,
        buildingSymbol = buildingSymbol,
        isManualCorrection = false,
        buildingName = buildingName,
    )
