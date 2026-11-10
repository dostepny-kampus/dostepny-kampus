/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import android.Manifest
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/** Ekran V4: krok 2 przekazuje grupę i piętro dalej, a potwierdzenie prowadzi do mapy (US4.1, US4.4, US4.5). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class QuickReportScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val folder = TemporaryFolder()

    private val next = mutableListOf<ReportRoute>()
    private var toMap = 0
    private var backs = 0
    private val actions =
        QuickReportActions(onNext = { next.add(it) }, onSent = {}, onBack = { backs++ }, onBackToMap = { toMap++ })
    private val atGroup = ReportRoute(step = ReportStep.GROUP, photoId = "zdjecie-1", photoPath = "/zdjecie-1.jpg")

    @Test
    fun `grupa z piętrem do kroku 3`() {
        show(atGroup)
        compose.onNodeWithText("Krok 2 z 3").assertIsDisplayed()
        compose.onNodeWithText("Toalety").performClick()
        compose.onNodeWithText("Parter").performClick()
        val expected = atGroup.copy(group = BarrierGroup.TOILETS, floorNumber = 0, isFloorAsked = true)
        assertEquals(listOf(expected), next)
    }

    @Test
    fun `grupa bez piętra do kroku 3`() {
        show(atGroup)
        compose.onNodeWithText("Coś innego").performClick()
        assertEquals(listOf(atGroup.copy(group = BarrierGroup.OTHER)), next)
    }

    @Test
    fun `potwierdzenie wraca do mapy`() {
        show(ReportRoute(step = ReportStep.SENT))
        compose.onNodeWithText("Wróć do mapy").performClick()
        assertEquals(1, toMap)
    }

    /** Wiersz: V4 krok 1 ze zdjęciem, „Wstecz” → potwierdzenie rezygnacji, US4.8. */
    @Test
    fun us4_8_leave_after_confirm() {
        val photo = folder.newFile("zdjecie-1.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        show(ReportRoute(step = ReportStep.PHOTO, photoId = "zdjecie-1", photoPath = photo.path))
        compose.onNodeWithText("Wstecz").performClick()
        compose.onNodeWithText("Wróć do zdjęcia").performClick()
        compose.onNodeWithText("Krok 1 z 3").assertIsDisplayed()
        assertEquals(0, backs)
        assertTrue(photo.exists())
        compose.onNodeWithText("Wstecz").performClick()
        compose.onNodeWithText("Tak, zrezygnuj").performClick()
        assertEquals(1, backs)
        assertFalse(photo.exists())
    }

    /** Wiersz: V4 krok 1 po „Zgłoś barierę przy wejściu”, napis z wejściem, US4.6. */
    @Test
    fun us4_6_entrance_line() {
        show(ReportRoute(entranceId = "node/6"))
        compose.onNodeWithText("Wejście: „G”").assertIsDisplayed()
        compose.onNodeWithText("Zrób zdjęcie").assertIsDisplayed()
    }

    /** Wiersz: V4 brak pozycji → „Zrezygnuj” pyta o rezygnację, zdjęcie zostaje do decyzji, potem mapa, UC7 4c. */
    @Test
    fun uc7_4c_give_up_no_position() {
        val photo = folder.newFile("zdjecie-1.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        show(atSend(photo.path))
        compose.onNodeWithText("Telefon nie ustalił", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Zrezygnuj").performClick()
        compose.onNodeWithText("Wróć do zdjęcia").performClick()
        compose.onNodeWithText("Spróbuj ponownie").assertIsDisplayed()
        assertTrue(photo.exists())
        compose.onNodeWithText("Zrezygnuj").performClick()
        compose.onNodeWithText("Tak, zrezygnuj").performClick()
        assertEquals(1, toMap)
        assertFalse(photo.exists())
    }

    @Test
    fun `krok 1 z mapy bez wejścia`() {
        show(ReportRoute())
        compose.onNodeWithText("Wejście", substring = true).assertDoesNotExist()
    }

    private fun show(route: ReportRoute) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        val transcoder = PhotoTranscoder()
        val catalog = BuildingCatalogRepository(EmptyCoroutineContext) { CAMPUS }
        val entrances = QuickEntranceViewModel(catalog)
        val send =
            QuickSendViewModel(
                positions = PositionSource { flow { throw SecurityException("brak zgody") } },
                locationSwitch = LocationSwitch { flowOf(true) },
                catalog = catalog,
                saver = { _, _, _ -> },
            )
        compose.setContent {
            DostepnyKampusTheme {
                QuickReportScreen(
                    route = route,
                    actions = actions,
                    models = QuickReportModels(photoTranscoder = transcoder, entrances = entrances, send = send),
                )
            }
        }
    }

    private fun atSend(path: String) =
        ReportRoute(step = ReportStep.SEND, photoId = "zdjecie-1", photoPath = path, group = BarrierGroup.TOILETS)

    private companion object {
        const val CAMPUS =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap","features":[""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"node/6",""" +
                """"entrance":"yes","ref":"G"},"geometry":{"type":"Point","coordinates":[17.06,51.11]}}]}"""
    }
}
