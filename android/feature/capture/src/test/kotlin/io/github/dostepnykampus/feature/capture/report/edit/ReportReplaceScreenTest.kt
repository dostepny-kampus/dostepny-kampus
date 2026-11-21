/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.edit

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.feature.capture.navigation.ReportEditRoute
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.report.ReportEditExits
import io.github.dostepnykampus.feature.capture.report.ReportFormViewModel
import io.github.dostepnykampus.feature.capture.report.form.ReportEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** C2 w trybie zmiany zgłoszenia przyjętego z V2. Wymagania: BR-3, BR-18 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ReportReplaceScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var left = 0
    private var saved = 0
    private val savedEntries = mutableListOf<Pair<SubmissionContent, EditedSubmission?>>()
    private lateinit var viewModel: ReportFormViewModel

    private fun show(barrier: AcceptedBarrier?) {
        val loader = ReportEditViewModel(edits = { null }, barriers = { listOfNotNull(barrier) })
        viewModel = ReportFormViewModel { content, _, edited -> savedEntries.add(content to edited) }
        val exits = ReportEditExits(onLeave = { left++ }, onSave = { saved++ }, onChangePlace = {})
        val route = ReportEditRoute(submissionId = ID, isAccepted = true)
        val transcoder = PhotoTranscoder()
        compose.setContent {
            DostepnyKampusTheme {
                ReportEditScreen(
                    route = route,
                    exits = exits,
                    loader = loader,
                    viewModel = viewModel,
                    photoTranscoder = transcoder,
                )
            }
        }
        compose.waitForIdle()
    }

    /**
     * Wiersz: zmiana przyjętego zgłoszenia tworzy nowe zgłoszenie z powiązaniem i nową uciążliwością.
     * Scenariusz (częściowo): zgloszenia-zbierajacego.feature: Zmiana zgłoszenia przyjętego tworzy nowe zgłoszenie
     *
     * Wymagania: BR-3, BR-18
     */
    @Test
    fun savesLinkedNewSubmission() {
        show(BARRIER)
        compose.onNodeWithText("Popraw zgłoszenie").assertIsDisplayed()
        compose.onNodeWithText("Zapis utworzy nowe zgłoszenie, które znów sprawdzi Weryfikator.").assertIsDisplayed()
        compose.onNodeWithText("Wysoki próg").performScrollTo().assertIsDisplayed()

        viewModel.edit(ReportEdit.Rating(Severity.BLOCKING))
        viewModel.edit(ReportEdit.Photo(PendingPhotoDraft(photoId = "p-new", path = "/tmp/p-new.jpg")))
        compose.onNodeWithText("Zapisz").performClick()
        compose.waitForIdle()

        val (content, edited) = savedEntries.single()
        assertEquals("zapis kończy ekran", 1, saved)
        assertEquals("powiązanie z przyjętym", ID, content.replacesSubmissionId)
        assertEquals("nowa uciążliwość", Severity.BLOCKING.value, content.severity)
        assertNull("nowe zgłoszenie bez edycji starego", edited)
    }

    /** Wiersz: brak zgłoszenia przyjętego → powrót bez otwierania formularza. Wymagania: BR-18 */
    @Test
    fun goesBackWhenAcceptedMissing() {
        show(null)
        assertEquals("powrót bez danych", 1, left)
        compose.onNodeWithText("Popraw zgłoszenie").assertDoesNotExist()
    }

    private companion object {
        const val ID = "tb-13-accepted"
        val BARRIER =
            AcceptedBarrier(
                id = ID,
                location = GeoPoint(longitude = 17.06, latitude = 51.11),
                severity = Severity.ONLY_WITH_HELP,
                type = BarrierType.THRESHOLD,
                buildingSymbol = "C-13",
                verifiedAt = null,
                floor = SubmissionFloor(number = 0, isMezzanine = false),
                description = "Wysoki próg",
            )
    }
}
