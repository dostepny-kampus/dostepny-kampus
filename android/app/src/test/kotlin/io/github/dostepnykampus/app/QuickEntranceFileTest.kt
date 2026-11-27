/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.data.building.BUILDINGS_ASSET
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.BuildingFloors
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.feature.capture.location.BuildingLookup
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.quick.QuickEntranceViewModel
import io.github.dostepnykampus.feature.capture.quick.QuickLookup
import io.github.dostepnykampus.feature.capture.quick.QuickReport
import io.github.dostepnykampus.feature.capture.quick.QuickReportActions
import io.github.dostepnykampus.feature.capture.quick.QuickReportModels
import io.github.dostepnykampus.feature.capture.quick.QuickReportScreen
import io.github.dostepnykampus.feature.capture.quick.QuickSendStage
import io.github.dostepnykampus.feature.capture.quick.QuickSendViewModel
import io.github.dostepnykampus.feature.capture.report.ReportSaver
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Zgłoszenie szybkie z „Zgłoś barierę przy wejściu” na prawdziwym katalogu PWr: wejście główne budynku A-7 bez
 * nazwy w danych.
 *
 * Wymagania: US4.6, BR-8, BR-14
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class QuickEntranceFileTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = RuntimeEnvironment.getApplication()
    private val catalog =
        BuildingCatalogRepository(EmptyCoroutineContext) {
            app.assets
                .open(BUILDINGS_ASSET)
                .bufferedReader()
                .use { it.readText() }
        }
    private val saved = mutableListOf<SubmissionContent>()
    private var positionReads = 0

    /**
     * Zgłoszenie z arkusza wejścia ma punkt tego wejścia i jego identyfikator, bez piętra i bez pozycji telefonu.
     * Scenariusz: zgloszenie-szybkie.feature: Zgłoszenie przy wejściu ma miejsce w punkcie wybranego wejścia
     */
    @Test
    fun reportsAtEntrancePoint() {
        val viewModel = viewModel()
        showLookup(viewModel)
        waitFor { viewModel.lookup.value is QuickLookup.Found }

        viewModel.send(QuickReport(photo = photo(), group = BarrierGroup.ENTRANCE_AND_DOORS, entranceId = ENTRANCE_ID))
        waitFor { viewModel.stage.value == QuickSendStage.SENT }

        val position = (viewModel.lookup.value as QuickLookup.Found).position
        assertEquals("miejsce to budynek wejścia", ENTRANCE_BUILDING, position.buildingSymbol)
        val content = saved.single()
        assertEquals("zgłoszenie ma punkt wejścia", ENTRANCE_AT, content.location)
        assertEquals("zgłoszenie ma identyfikator wejścia", ENTRANCE_ID, content.entranceId)
        assertNull("zgłoszenie bez piętra, bo wejście nie ma piętra", content.floor)
        assertEquals("telefon nie ustala bieżącej pozycji", 0, positionReads)
    }

    /**
     * Krok 1 pokazuje napis wejścia z danych: wejście główne bez nazwy to „Wejście: główne”.
     * Scenariusz: zgloszenie-szybkie.feature: Zgłoszenie przy wejściu ma miejsce w punkcie wybranego wejścia
     */
    @Test
    fun showsMainEntranceName() {
        val viewModel = viewModel()
        val entrances = QuickEntranceViewModel(catalog)
        val transcoder = PhotoTranscoder()
        compose.setContent {
            DostepnyKampusTheme {
                QuickReportScreen(
                    route = ReportRoute(entranceId = ENTRANCE_ID),
                    actions = QuickReportActions(onNext = {}, onSent = {}, onBack = {}, onBackToMap = {}),
                    models =
                        QuickReportModels(
                            photoTranscoder = transcoder,
                            entrances = entrances,
                            send = viewModel,
                        ),
                )
            }
        }

        compose.waitUntil(WAIT_MS) { compose.onAllNodes(hasText(MAIN_ENTRANCE)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(MAIN_ENTRANCE).assertIsDisplayed()
    }

    private fun showLookup(viewModel: QuickSendViewModel) {
        compose.setContent {
            val lookup by viewModel.lookup.collectAsState()
            Text(lookup.toString())
        }
    }

    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(WAIT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun viewModel(): QuickSendViewModel =
        QuickSendViewModel(
            positions =
                PositionSource {
                    positionReads++
                    flowOf(PositionFix(OUTSIDE, ACCURACY, elapsedMillis = 0))
                },
            locationSwitch = LocationSwitch { flowOf(true) },
            saver = ReportSaver { content, _, _ -> saved += content },
            networkStatus = NetworkStatus { flowOf(true) },
            buildingFloors = BuildingFloors { null },
            buildingLookup = BuildingLookup(catalog, EmptyCoroutineContext),
        ).apply { start(entranceId = ENTRANCE_ID) }

    private fun photo() = photoDraft(app.cacheDir, "p-wejscie")
}

private const val ENTRANCE_ID = "node/13151513134"
private const val ENTRANCE_BUILDING = "A-7"
private const val MAIN_ENTRANCE = "Wejście: główne"
private const val WAIT_MS = 5_000L
private const val ACCURACY = 6.0
private val ENTRANCE_AT = GeoPoint(longitude = 17.0619164, latitude = 51.1082447)
private val OUTSIDE = GeoPoint(longitude = 17.059, latitude = 51.109)
