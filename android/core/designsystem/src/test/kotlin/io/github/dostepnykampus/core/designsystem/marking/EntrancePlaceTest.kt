/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Miejsce zgłoszenia słowami od najbliższego wejścia, bez współrzędnych. Wymagania: US7.4, NFR-19 */
@RunWith(RobolectricTestRunner::class)
class EntrancePlaceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz: wejście z nazwą, odległość zaokrąglona i dokładność z przecinkiem. Wymagania: US7.4, NFR-19 */
    @Test
    fun namesEntranceAndAccuracy() {
        val named = entrance(name = "Wejście Cesarskie", isMain = false)
        compose.setContent { Text(measuredPlace(near = nearEntrance(named, meters = 2.6), accuracyMeters = 0.8)) }
        compose.onNodeWithText("ok. 3 m od wejścia „Wejście Cesarskie”, dokładność 0,8 m").assertExists()
    }

    /** Wiersz: wejście główne bez nazwy, a bez wejścia sama dokładność. Wymagania: US7.4 */
    @Test
    fun namesMainOrOnlyAccuracy() {
        val main = entrance(name = null, isMain = true)
        compose.setContent {
            Text(nearEntrance(main, meters = 0.2))
            Text(measuredPlace(near = null, accuracyMeters = 15.0))
        }
        compose.onNodeWithText("ok. 1 m od wejścia głównego").assertExists()
        compose.onNodeWithText("dokładność 15,0 m").assertExists()
    }

    private fun entrance(name: String?, isMain: Boolean): Entrance =
        Entrance(
            id = "e",
            buildingSymbol = "A-1",
            name = name,
            isMain = isMain,
            type = null,
            location = GeoPoint(longitude = 17.06, latitude = 51.1),
            marking = EntranceMarking.ACCESSIBLE,
        )
}
