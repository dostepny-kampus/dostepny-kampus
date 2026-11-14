/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report

import io.github.dostepnykampus.core.model.GeoPoint

/** Miejsce z pomiaru C1 przy Gmachu Głównym A-1, wspólne dla testów formularza C2. */
internal val A1 =
    ReportPlace(
        location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
        accuracyMeters = 0.8,
        buildingSymbol = "A-1",
        isManualCorrection = false,
    )
