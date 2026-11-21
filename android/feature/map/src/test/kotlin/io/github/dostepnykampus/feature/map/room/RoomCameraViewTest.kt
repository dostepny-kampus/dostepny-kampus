/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.rememberCameraView
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.plan.MapPanel
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.FOUND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.VIA_LIFT
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Zamknięcie karty i dotknięcie sali bez zbędnego ruchu mapy. Wymagania: US18.1, US18.2, NFR-56 */
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

    /** Karta dotkniętej sali otwarta chwilę po dotknięciu, gdy czeka na drogę, zostawia powiększenie i obraz. */
    @Test
    fun keepsPictureForLateCard() {
        val idle = MapCamera(center = GeoPoint(longitude = 17.05, latitude = 51.11), zoom = 20.5)
        val focus =
            MapFocus.Building(symbol = FOUND.building, command = 1L, floor = FOUND.floor.id, room = FOUND.room.id)
        val room = mutableStateOf<OpenRoom?>(null)
        val cover = mutableIntStateOf(0)
        lateinit var view: RoomView
        compose.setContent {
            view =
                rememberRoomView(
                    scene = RoomScene(room = room.value, isOnMap = true, plan = null),
                    focus = focus.takeIf { room.value != null },
                    panel =
                        MapPanel(cover = cover.intValue, wayCover = cover.intValue.takeIf { room.value != null } ?: 0),
                    height = 2000,
                )
        }

        compose.runOnIdle {
            view.onIdle(idle)
            view.tapped {}(FOUND)
            cover.intValue = 1
        }
        compose.runOnIdle {
            room.value = OpenRoom(card = RoomCard(found = FOUND, way = VIA_LIFT), onClose = {}, onStep = {})
            cover.intValue = 600
        }
        compose.runOnIdle {
            val camera = view.camera
            assertEquals("Powiększenie się zmieniło", idle.zoom, camera?.zoom ?: 0.0, 1e-9)
            assertEquals("Mapa ruszyła w bok", idle.center.longitude, camera?.run { center.longitude } ?: 0.0, 1e-9)
        }
    }
}
