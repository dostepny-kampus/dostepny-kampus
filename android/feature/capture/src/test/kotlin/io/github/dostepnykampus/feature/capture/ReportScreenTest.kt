/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Ekran C2: miejsce z C1, piętro liczbą, typ bariery z listy, uciążliwość i „Zapisz” (US7.1, US7.2, US7.4). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ReportScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val edits = mutableListOf<ReportEdit>()
    private var saves = 0

    private fun show(place: ReportPlace = A1, replaces: String? = null) {
        val state = ReportUiState(form = ReportForm(place = place, replacesSubmissionId = replaces))
        val actions = ReportActions(onEdit = { edits.add(it) }, onChangePlace = {}, onSave = { saves++ }, onBack = {})
        compose.setContent { DostepnyKampusTheme { ReportContent(state = state, actions = actions) } }
    }

    @Test
    fun us7_4_place_no_coordinates() {
        show()

        compose.onNodeWithText("Opisz barierę").assertIsDisplayed()
        compose.onNodeWithText("Budynek A-1").assertIsDisplayed()
        compose.onNodeWithText("dokładność 0,8 m").assertIsDisplayed()
    }

    @Test
    fun us8_3_edit_header() {
        show(replaces = "s-old")

        compose.onNodeWithText("Popraw zgłoszenie").assertIsDisplayed()
    }

    @Test
    fun brg4_no_floor_outside() {
        show(place = A1.copy(buildingSymbol = null))

        compose.onNodeWithText("Poza budynkiem").assertIsDisplayed()
        assertEquals(0, compose.onAllNodes(hasText("Piętro")).fetchSemanticsNodes().size)
    }

    @Test
    fun us7_1_severity_and_save() {
        show()
        compose.onNodeWithText("Blokada").performScrollTo().performClick()
        compose.onNodeWithText("Zapisz").performClick()

        assertEquals(ReportEdit.Rating(Severity.BLOCKING), edits.last())
        assertEquals(1, saves)
    }

    @Test
    fun us7_2_floor_number() {
        show()
        compose.onNodeWithText("Numer piętra").performTextInput("-1")

        assertEquals(ReportEdit.Floor(ReportFloor.Numbered(number = -1, isMezzanine = false)), edits.last())
    }

    @Test
    fun us7_2_floor_not_integer() {
        show()
        compose.onNodeWithText("Numer piętra").performTextInput("1,5")

        assertEquals(ReportEdit.Floor(null), edits.last())
        compose.onNodeWithText("Wpisz liczbę całkowitą, np. 0 albo -1.").assertIsDisplayed()
    }

    @Test
    fun us7_1_type_from_list() {
        show()
        compose.onNodeWithText("Wybierz typ bariery").performClick()
        compose.onNodeWithText("Próg").performScrollTo().performClick()

        assertEquals(ReportEdit.Type(BarrierType.THRESHOLD), edits.last())
    }
}
