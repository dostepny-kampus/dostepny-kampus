/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.quick

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.AppShell
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.photoDraft
import io.github.dostepnykampus.core.data.building.BUILDINGS_ASSET
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.BuildingFloors
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.feature.capture.location.BuildingLookup
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.photo.LocalPhotoDecodeContext
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.quick.QuickReportActions
import io.github.dostepnykampus.feature.capture.quick.QuickReportModels
import io.github.dostepnykampus.feature.capture.quick.QuickReportScreen
import io.github.dostepnykampus.feature.capture.quick.QuickSendViewModel
import io.github.dostepnykampus.feature.capture.report.ReportSaver
import kotlinx.coroutines.flow.flowOf
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

/** Punkt wewnątrz konturu budynku D-2 z prawdziwego katalogu PWr. */
internal val INSIDE_D2 = GeoPoint(longitude = 17.05758, latitude = 51.10978)

/** Dokładność pozycji telefonu w testach zgłoszenia szybkiego, w metrach. */
internal const val FIX_ACCURACY = 6.0

internal fun fixedAt(point: GeoPoint) = PositionSource { flowOf(PositionFix(point, FIX_ACCURACY, elapsedMillis = 0)) }

/**
 * Krok 3 V4 na prawdziwym katalogu budynków PWr: telefon ustala [point] przez [positions], a zapis idzie do [saver]
 * z siecią [networkStatus].
 */
internal fun Context.quickSendModel(
    point: GeoPoint,
    networkStatus: NetworkStatus,
    saver: ReportSaver,
    positions: PositionSource = fixedAt(point),
): QuickSendViewModel {
    val catalog =
        BuildingCatalogRepository(EmptyCoroutineContext) {
            assets.open(BUILDINGS_ASSET).bufferedReader().use { it.readText() }
        }
    return QuickSendViewModel(
        positions = positions,
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
    CompositionLocalProvider(LocalPhotoDecodeContext provides EmptyCoroutineContext) {
        QuickReportScreen(route = route, actions = actions, models = models)
    }
}

/** Modele V4 z siecią i zapisem do [saved]; telefon stoi w [point], a pozycje podaje [positions]. */
internal fun Context.quickModels(
    point: GeoPoint,
    saved: MutableList<SubmissionContent>,
    positions: PositionSource = fixedAt(point),
): QuickReportModels {
    val saver = ReportSaver { content, _, _ -> saved += content }
    val send = quickSendModel(point = point, networkStatus = { flowOf(true) }, saver = saver, positions = positions)
    return QuickReportModels(photoTranscoder = PhotoTranscoder(), send = send)
}

/** Powłoka aplikacji Odwiedzającego: V4 z modelami [models], inne trasy rysuje [others]; zwraca nawigator. */
internal fun ComposeContentTestRule.showQuickApp(
    models: QuickReportModels,
    others: @Composable AppNavigator.(NavKey) -> Unit = {},
): AppNavigator {
    var navigator: AppNavigator? = null
    setContent {
        DostepnyKampusTheme {
            AppShell(firstLaunch = false, roles = TestRoles(Account.SignedIn()), onFirstHelpLeft = {}) { route ->
                navigator = this
                if (route is ReportRoute) QuickTestScreen(route, models) else others(route)
            }
        }
    }
    return checkNotNull(navigator)
}

/** Czeka na [condition], aż ustali się pozycja albo wczyta katalog. */
internal fun ComposeContentTestRule.waitFor(condition: () -> Boolean) = waitUntil(timeoutMillis = 10_000L, condition)

/** Krok 1 V4 po zdjęciu bariery zapisanym w [dir]: [navigator] przechodzi do kroku 2 z danymi trasy. */
internal fun ComposeContentTestRule.takePhoto(navigator: AppNavigator, dir: File) {
    val photo = photoDraft(dir, "zdjecie-bariery")
    navigator.nextReportStep((navigator.current as ReportRoute).copy(photoId = photo.photoId, photoPath = photo.path))
}
