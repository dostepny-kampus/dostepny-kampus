/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.quick

import android.content.Context
import androidx.compose.runtime.Composable
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.core.data.building.BUILDINGS_ASSET
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.BuildingFloors
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.capture.location.BuildingLookup
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.quick.QuickReportActions
import io.github.dostepnykampus.feature.capture.quick.QuickReportModels
import io.github.dostepnykampus.feature.capture.quick.QuickReportScreen
import io.github.dostepnykampus.feature.capture.quick.QuickSendViewModel
import io.github.dostepnykampus.feature.capture.report.ReportSaver
import kotlinx.coroutines.flow.flowOf
import kotlin.coroutines.EmptyCoroutineContext

/** Punkt wewnątrz konturu budynku D-2 z prawdziwego katalogu PWr. */
internal val INSIDE_D2 = GeoPoint(longitude = 17.05758, latitude = 51.10978)

/** Dokładność pozycji telefonu w testach zgłoszenia szybkiego, w metrach. */
internal const val FIX_ACCURACY = 6.0

/**
 * Krok 3 V4 na prawdziwym katalogu budynków PWr: telefon ustala [point], a zapis idzie do [saver]
 * z siecią [networkStatus].
 */
internal fun Context.quickSendModel(
    point: GeoPoint,
    networkStatus: NetworkStatus,
    saver: ReportSaver,
): QuickSendViewModel {
    val catalog =
        BuildingCatalogRepository(EmptyCoroutineContext) {
            assets.open(BUILDINGS_ASSET).bufferedReader().use { it.readText() }
        }
    return QuickSendViewModel(
        positions = PositionSource { flowOf(PositionFix(point, FIX_ACCURACY, elapsedMillis = 0)) },
        locationSwitch = LocationSwitch { flowOf(true) },
        saver = saver,
        networkStatus = networkStatus,
        buildingFloors = BuildingFloors { null },
        buildingLookup = BuildingLookup(catalog, EmptyCoroutineContext),
    ).apply { start(entranceId = null) }
}

/** V4 na kroku z [route] z czynnościami podpiętymi do nawigatora i modelami [models] spoza grafu Hilt. */
@Composable
internal fun AppNavigator.QuickTestScreen(route: ReportRoute, models: QuickReportModels) {
    val actions =
        QuickReportActions(
            onNext = { nextReportStep(it) },
            onSent = { reportSent(isSavedOffline = it) },
            onBack = { back() },
            onBackToMap = { backToMap() },
        )
    QuickReportScreen(route = route, actions = actions, models = models)
}
