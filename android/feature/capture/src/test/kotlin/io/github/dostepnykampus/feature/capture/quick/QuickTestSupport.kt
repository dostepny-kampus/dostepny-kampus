/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.BuildingFloors
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.feature.capture.location.BuildingLookup
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.photo.LocalPhotoDecodeContext
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.report.ReportSaver
import kotlinx.coroutines.flow.flowOf
import kotlin.coroutines.EmptyCoroutineContext

internal val QUICK_PWR_FLOORS: Map<String, List<String>> =
    mapOf(
        "D-2" to listOf("Parter", "1 piętro", "2 piętro", "3 piętro", "4 piętro"),
        "C-13" to listOf("Parter", "1 piętro", "2 piętro"),
    )

internal fun quickPwrFloors(): BuildingFloors =
    BuildingFloors { symbol ->
        QUICK_PWR_FLOORS[symbol]?.let { labels ->
            FloorList(
                source = "PWr",
                floors =
                    labels.mapIndexed { index, label ->
                        Floor(id = "$symbol/p$index", label = label, order = index)
                    },
            )
        }
    }

private fun quickCatalog() = BuildingCatalogRepository(EmptyCoroutineContext) { QUICK_CAMPUS }

internal fun quickSendModel(
    positions: PositionSource,
    saver: ReportSaver,
    floors: BuildingFloors = BuildingFloors { null },
): QuickSendViewModel =
    QuickSendViewModel(
        positions = positions,
        locationSwitch = LocationSwitch { flowOf(true) },
        saver = saver,
        networkStatus = NetworkStatus { flowOf(true) },
        buildingFloors = floors,
        buildingLookup = BuildingLookup(quickCatalog(), EmptyCoroutineContext),
    )

internal fun ComposeContentTestRule.showQuickReport(
    route: State<ReportRoute>,
    actions: QuickReportActions,
    send: QuickSendViewModel,
    barriers: AcceptedBarriers = AcceptedBarriers { emptyList() },
) {
    val models =
        QuickReportModels(
            photoTranscoder = PhotoTranscoder(),
            entrances = QuickEntranceViewModel(quickCatalog()),
            send = send,
            offline = QuickOfflineViewModel(catalog = quickCatalog(), barriers = barriers),
        )
    setContent {
        DostepnyKampusTheme {
            CompositionLocalProvider(LocalPhotoDecodeContext provides EmptyCoroutineContext) {
                QuickReportScreen(route = route.value, actions = actions, models = models)
            }
        }
    }
}
