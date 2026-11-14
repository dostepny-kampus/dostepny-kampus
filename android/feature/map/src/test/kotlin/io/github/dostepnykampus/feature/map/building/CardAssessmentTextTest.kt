/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.server.ServerCard
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.testing.serverEntrances
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.AssessmentSource
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.model.EntranceAssessment
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.coroutines.EmptyCoroutineContext

/** Od odpowiedzi serwera o budynku do napisu o dniu i źródle oznaczenia na V2. Wymagania: US17.3 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CardAssessmentTextTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: odpowiedź serwera `GET /buildings/A-1` z wejściami z importu z dnia 1.10.2026 → na V2 wiersz
     *   „Z map i opisów, stan z 1.10.2026”, import.
     *
     * Wymagania: US17.3, BR-24
     */
    @Test
    fun us17_3_server_answer_to_screen() {
        val viewModel =
            BuildingCardViewModel(
                symbol = "A-1",
                repository = BuildingCatalogRepository(EmptyCoroutineContext) { ASSET_JSON },
                networkStatus = { flowOf(true) },
                serverEntrances = serverEntrances { ServerReply.Ok(DETAILS) },
            )
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardScreen(
                    symbol = "A-1",
                    mode = CardMode.Expanded,
                    onCheckEntrances = {},
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    viewModel = viewModel,
                )
            }
        }
        compose.waitUntil { compose.onAllNodes(hasText("Główne")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Z map i opisów, stan z 1.10.2026").assertExists()
    }

    private companion object {
        val SERVER_ENTRANCE =
            Entrance(
                id = "a1-main",
                buildingSymbol = "A-1",
                name = "Główne",
                isMain = true,
                type = null,
                location = GeoPoint(longitude = 0.5, latitude = 0.0),
                marking = EntranceMarking.ACCESSIBLE,
                assessment = EntranceAssessment(LocalDate.of(2026, 10, 1), AssessmentSource.IMPORT),
            )

        val DETAILS =
            ServerCard(
                building = Building(symbol = "A-1", name = "Gmach", outline = GeoPolygon(emptyList())),
                entrances = listOf(SERVER_ENTRANCE),
            )

        const val ASSET_JSON =
            """{"type":"FeatureCollection","attribution":"©","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"display_name":"Gmach",""" +
                """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
    }
}
