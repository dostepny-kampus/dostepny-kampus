/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.rememberCameraView
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.FOUND
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Zamknięcie karty bez ręcznego przesuwania mapy. Wymagania: US18.2 */
@RunWith(RobolectricTestRunner::class)
class RoomCameraViewTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun keepsRoomCameraWhenCardCloses() {
        val focus =
            MapFocus.Building(symbol = FOUND.building, command = 1L, floor = FOUND.floor.id, room = FOUND.room.id)
        val target = MapCamera(center = FOUND.room.labelPoint, zoom = 19.0)
        val open = mutableStateOf(true)
        var shown: MapCamera? = null
        compose.setContent {
            val view =
                rememberCameraView(
                    focus = focus.takeIf { open.value },
                    layers = null,
                    roomCamera = target.takeIf { open.value },
                )
            SideEffect { shown = view.camera }
        }

        compose.runOnIdle { open.value = false }
        compose.runOnIdle { assertEquals("Kamera zostaje nad salą po zamknięciu", target, shown) }
    }
}
