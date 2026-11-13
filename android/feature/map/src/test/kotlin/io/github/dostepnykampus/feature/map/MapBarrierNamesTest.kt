/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.data.barriers.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Severity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Nazwy znaków barier dla TalkBack na mapie V1. Wymagania: US1.3, NFR-18 */
@RunWith(RobolectricTestRunner::class)
class MapBarrierNamesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val stairs = barrier(Severity.BLOCKING, type = BarrierType.STAIRS_AT_ENTRANCE, symbol = "A-1")
    private val curb = barrier(Severity.HINDRANCE, type = BarrierType.CURB, symbol = null)
    private val untyped = barrier(Severity.ONLY_WITH_HELP, type = null, symbol = "C-13")

    /**
     * Wiersz: V1, znak bariery ma nazwę dla TalkBack, np. „Blokada: schody przy wejściu, budynek A-1”.
     *
     * Wymagania: NFR-18
     */
    @Test
    fun nfr18_sign_names() {
        compose.setContent { Screen() }

        compose.onNodeWithContentDescription("Blokada: schody przy wejściu, budynek A-1").assertExists()
        compose.onNodeWithContentDescription("Utrudnienie: krawężnik, poza budynkiem").assertExists()
        compose.onNodeWithContentDescription("Tylko z pomocą, budynek C-13").assertExists()
    }

    /** Wiersz: mapa nad małą kartą, TalkBack nie czyta mapy pod kartą. Wymagania: NFR-19 */
    @Test
    fun nfr19_no_names_under_card() {
        compose.setContent { Screen(sheet = 300.dp) }

        compose.onNodeWithContentDescription("Blokada: schody przy wejściu, budynek A-1").assertDoesNotExist()
    }

    @Composable
    private fun Screen(sheet: Dp = 0.dp) {
        DostepnyKampusTheme {
            CampusMapScreen(
                state = CampusMapUiState(isListSaved = false, barriers = listOf(stairs, curb, untyped)),
                onListChange = {},
                onSearch = {},
                onBuildingClick = {},
                contentPadding = PaddingValues(bottom = sheet),
                mapView = { _, _, _, _, _, _, _ -> },
            )
        }
    }

    private fun barrier(severity: Severity, type: BarrierType?, symbol: String?) =
        AcceptedBarrier(
            id = severity.name,
            location = GeoPoint(longitude = 0.0, latitude = 0.0),
            severity = severity,
            type = type,
            buildingSymbol = symbol,
            verifiedAt = null,
        )
}
