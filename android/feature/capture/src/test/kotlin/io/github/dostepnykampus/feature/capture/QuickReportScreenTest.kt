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
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Ekran V4: krok 2 przekazuje grupę i piętro dalej, a potwierdzenie prowadzi do mapy (US4.1, US4.4, US4.5). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class QuickReportScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val next = mutableListOf<ReportRoute>()
    private var toMap = 0
    private val actions =
        QuickReportActions(onNext = { next.add(it) }, onSent = {}, onBack = {}, onBackToMap = { toMap++ })
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

    private fun show(route: ReportRoute) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        compose.setContent { DostepnyKampusTheme { QuickReportScreen(route = route, actions = actions) } }
    }
}
