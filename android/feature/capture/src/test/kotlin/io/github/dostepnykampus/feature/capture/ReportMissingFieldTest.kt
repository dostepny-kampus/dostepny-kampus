/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * „Zapisz” na C2 z jednym brakującym polem: ekran z modelem widoku pokazuje komunikat o tym polu, a zapis nie
 * dostaje zgłoszenia.
 *
 * Wymagania: US7.1, US7.3, BR-7
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ReportMissingFieldTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var saves = 0
    private val viewModel = ReportFormViewModel { _, _, _ -> saves++ }
    private val floor = ReportEdit.Floor(ReportFloor.Numbered(0, isMezzanine = false))
    private val type = ReportEdit.Type(BarrierType.STAIRS_AT_ENTRANCE)
    private val rating = ReportEdit.Rating(Severity.BLOCKING)
    private val photo = ReportEdit.Photo(PendingPhotoDraft(photoId = "p1", path = "/data/p1.jpg"))

    /**
     * Bez typu bariery: komunikat o typie, nic nie zapisane.
     * Scenariusz (przykład: typu bariery): pomiar-precyzyjny.feature: Bez typu bariery, uciążliwości albo zdjęcia
     *   zgłoszenia nie da się zapisać
     */
    @Test
    fun us7_1_no_type_not_saved() {
        saveWith(floor, rating, photo)
        assertOnlyMessage("Brakuje typu bariery. Uzupełnij to pole, aby zapisać.")
    }

    /**
     * Bez uciążliwości: komunikat o uciążliwości, nic nie zapisane.
     * Scenariusz (przykład: uciążliwości): pomiar-precyzyjny.feature: Bez typu bariery, uciążliwości albo zdjęcia
     *   zgłoszenia nie da się zapisać
     */
    @Test
    fun us7_1_no_severity_not_saved() {
        saveWith(floor, type, photo)
        assertOnlyMessage("Brakuje uciążliwości. Uzupełnij to pole, aby zapisać.")
    }

    /**
     * Bez zdjęcia: komunikat o zdjęciu, nic nie zapisane.
     * Scenariusz (przykład: zdjęcia): pomiar-precyzyjny.feature: Bez typu bariery, uciążliwości albo zdjęcia zgłoszenia
     *   nie da się zapisać
     */
    @Test
    fun us7_1_no_photo_not_saved() {
        saveWith(floor, type, rating)
        assertOnlyMessage("Brakuje zdjęcia. Zrób zdjęcie, aby zapisać.")
    }

    /**
     * Budynek A-1 bez piętra: komunikat o piętrze, nic nie zapisane.
     * Scenariusz: pomiar-precyzyjny.feature: W budynku trzeba podać piętro
     */
    @Test
    fun br7_no_floor_not_saved() {
        saveWith(type, rating, photo)
        assertOnlyMessage("Brakuje piętra. Uzupełnij to pole, aby zapisać.")
    }

    /**
     * TB-29 bez opisu: komunikat o opisie, nic nie zapisane.
     * Scenariusz: pomiar-precyzyjny.feature: Typ TB-29 „inna bariera” wymaga opisu
     */
    @Test
    fun us7_3_other_no_description() {
        saveWith(floor, ReportEdit.Type(BarrierType.OTHER_BARRIER), ReportEdit.Rating(Severity.ONLY_WITH_HELP), photo)
        assertOnlyMessage("Brakuje opisu. Przy typie „inna bariera” opis jest obowiązkowy.")
    }

    private fun saveWith(vararg edits: ReportEdit) {
        val transcoder = PhotoTranscoder()
        compose.setContent {
            DostepnyKampusTheme {
                ReportFormScreen(
                    route =
                        ReportFormRoute(
                            longitude = 17.06209,
                            latitude = 51.10785,
                            accuracyMeters = 0.8,
                            buildingSymbol = "A-1",
                        ),
                    onLeave = {},
                    onSave = {},
                    viewModel = viewModel,
                    photoTranscoder = transcoder,
                )
            }
        }
        compose.waitForIdle()
        edits.forEach(viewModel::edit)
        compose.onNodeWithText("Zapisz").performClick()
        compose.waitForIdle()
    }

    private fun assertOnlyMessage(message: String) {
        val shown = compose.onAllNodesWithText("Brakuje", substring = true).fetchSemanticsNodes()
        assertEquals(BANNER_AND_FIELD, compose.onAllNodesWithText(message).fetchSemanticsNodes().size)
        assertEquals(BANNER_AND_FIELD, shown.size)
        assertEquals(ReportStage.EDITING, viewModel.uiState.value?.stage)
        assertEquals(0, saves)
    }
}

private const val BANNER_AND_FIELD = 2
