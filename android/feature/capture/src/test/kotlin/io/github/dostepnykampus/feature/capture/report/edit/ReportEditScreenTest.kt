/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.edit

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.data.submission.pending.PendingEdit
import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.feature.capture.navigation.ReportEditRoute
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.report.form.ReportFormViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * C2 w trybie poprawki z C3: pola z kolejki, zapis tego samego zgłoszenia i „Zmień miejsce” przez C1.
 *
 * Wymagania: US8.3
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ReportEditScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var left = 0
    private var saved = 0
    private var placeChanged = 0
    private val savedEdits = mutableListOf<EditedSubmission?>()
    private var loads = 0

    private fun show(edit: PendingEdit?) {
        val loader =
            ReportEditViewModel { id ->
                loads++
                edit?.takeIf { it.edited.id == id }
            }
        val viewModel = ReportFormViewModel { _, _, edited -> savedEdits.add(edited) }
        val transcoder = PhotoTranscoder()
        compose.setContent {
            DostepnyKampusTheme {
                ReportEditScreen(
                    route = ReportEditRoute(submissionId = ID),
                    exits =
                        ReportEditExits(
                            onLeave = { left++ },
                            onSave = { saved++ },
                            onChangePlace = { placeChanged++ },
                        ),
                    loader = loader,
                    viewModel = viewModel,
                    photoTranscoder = transcoder,
                )
            }
        }
        compose.waitForIdle()
    }

    /** Wiersz: C3 „Otwórz” → C2 z opisem zapisanego zgłoszenia. Wymagania: US8.3 */
    @Test
    fun fillsFormWithSavedFields() {
        show(edit())
        compose.onNodeWithText("Popraw zgłoszenie").assertIsDisplayed()
        compose.onNodeWithText(DESCRIPTION).performScrollTo().assertIsDisplayed()
        assertEquals(1, loads)
    }

    /** Wiersz: poprawka C2 „Zapisz” → to samo zgłoszenie w kolejce, powrót do C3. Wymagania: US8.3, BR-9 */
    @Test
    fun replacesSameSubmission() {
        show(edit())
        compose.onNodeWithText("Zapisz").performClick()
        compose.waitForIdle()
        assertEquals(listOf<EditedSubmission?>(EditedSubmission(ID, baseVersion = 2)), savedEdits)
        assertEquals(1, saved)
    }

    /** Wiersz: poprawka C2 „Zmień miejsce” → C1 z polami w szkicu. Wymagania: US7.4, US8.3 */
    @Test
    fun changesPlaceInEdit() {
        show(edit())
        compose.onNodeWithText("Zmień miejsce").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(1, placeChanged)
        assertEquals(0, left)
    }

    /** Wiersz: zgłoszenia nie ma już w telefonie → powrót do C3 bez formularza. Wymagania: US8.3 */
    @Test
    fun goesBackWhenMissing() {
        show(null)
        assertEquals(1, left)
        compose.onNodeWithText("Popraw zgłoszenie").assertDoesNotExist()
    }

    private fun edit(): PendingEdit =
        PendingEdit(
            edited = EditedSubmission(ID, baseVersion = 2),
            content =
                SubmissionContent(
                    kind = SubmissionKind.PRECISE,
                    location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
                    photoIds = listOf(PHOTO),
                    accuracyMeters = 0.8,
                    isManualCorrection = false,
                    buildingSymbol = "A-1",
                    floor = SubmissionFloor(number = 1, isMezzanine = false),
                    barrierType = "TB-1",
                    severity = 2,
                    description = DESCRIPTION,
                ),
            photos = listOf(PendingPhotoDraft(photoId = PHOTO, path = "/data/$PHOTO.jpg")),
        )
}

private const val ID = "s1"
private const val PHOTO = "p1"
private const val DESCRIPTION = "Wysoki próg"
