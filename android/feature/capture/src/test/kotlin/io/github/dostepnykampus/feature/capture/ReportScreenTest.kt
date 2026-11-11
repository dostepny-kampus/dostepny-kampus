/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import io.github.dostepnykampus.core.data.submissions.EditedSubmission
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.geo.EntranceDistance
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Ekran C2: miejsce z C1, piętro liczbą, typ bariery z listy, uciążliwość i „Zapisz” (US7.1, US7.2, US7.4). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ReportScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val folder = TemporaryFolder()

    private val edits = mutableListOf<ReportEdit>()
    private var saves = 0

    private var exits = 0

    private fun show(place: ReportPlace = A1, replaces: String? = null) {
        show(ReportUiState(form = ReportForm(place = place, replacesSubmissionId = replaces)))
    }

    private fun show(state: ReportUiState) {
        val actions =
            ReportActions(
                onEdit = { edits.add(it) },
                onChangePlace = {},
                camera = PhotoCamera(onTake = {}),
                onSave = { saves++ },
                onBack = {},
                onConfirmExit = { exits++ },
                onStay = {},
            )
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
    fun us7_4_place_in_words() {
        show(A1.copy(buildingName = "Gmach Główny", nearestEntrance = near(name = null, isMain = true, meters = 3.2)))

        compose.onNodeWithText("Budynek A-1 Gmach Główny").assertIsDisplayed()
        compose.onNodeWithText("ok. 3 m od wejścia głównego, dokładność 0,8 m").assertIsDisplayed()
    }

    @Test
    fun us7_4_named_entrance() {
        show(A1.copy(nearestEntrance = near(name = "Wejście Cesarskie", isMain = false, meters = 0.3)))

        compose.onNodeWithText("ok. 1 m od wejścia „Wejście Cesarskie”, dokładność 0,8 m").assertIsDisplayed()
    }

    @Test
    fun brg18_edit_header() {
        show(replaces = "s-old")

        compose.onNodeWithText("Popraw zgłoszenie").assertIsDisplayed()
    }

    @Test
    fun us8_3_edit_header() {
        val edited = EditedSubmission(id = "s1", baseVersion = 2)
        show(ReportUiState(form = ReportForm(place = A1, editedSubmission = edited)))

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
    fun us7_4_new_building_empty_floor() {
        val state = mutableStateOf(ReportUiState(form = ReportForm(place = A1)))
        val actions =
            ReportActions(
                onEdit = { edits.add(it) },
                onChangePlace = {},
                camera = PhotoCamera(onTake = {}),
                onSave = {},
                onBack = {},
                onConfirmExit = {},
                onStay = {},
            )
        compose.setContent { DostepnyKampusTheme { ReportContent(state = state.value, actions = actions) } }
        compose.onNodeWithText("Numer piętra").performTextInput("2")
        assertEquals(1, compose.onAllNodes(hasText("2")).fetchSemanticsNodes().size)

        state.value = ReportUiState(form = ReportForm(place = A1.copy(buildingSymbol = "C-13")))
        compose.waitForIdle()

        assertEquals(0, compose.onAllNodes(hasText("2")).fetchSemanticsNodes().size)
    }

    @Test
    fun us7_2_floor_from_plan() {
        val floors = listOf(PlanFloor(id = "A-1-p", label = "Piwnica"), PlanFloor(id = "A-1-3", label = "3 piętro"))
        show(A1.copy(floors = PlanFloors(floors, source = "Politechnika Wrocławska")))

        compose.onNodeWithText("Źródło: Politechnika Wrocławska.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("3 piętro").performScrollTo().performClick()

        assertEquals(ReportEdit.Floor(ReportFloor.Planned("A-1-3")), edits.last())
        assertEquals(0, compose.onAllNodes(hasText("Numer piętra")).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodes(hasText("Nie mamy planu", substring = true)).fetchSemanticsNodes().size)
    }

    @Test
    fun us7_1_type_from_list() {
        show()
        compose.onNodeWithText("Wybierz typ bariery").performClick()
        compose.onNodeWithText("Próg").performScrollTo().performClick()

        assertEquals(ReportEdit.Type(BarrierType.THRESHOLD), edits.last())
    }

    /** Lista typów: „Zamknij” zamyka ją bez zmiany typu, a fokus wraca na pole (NFR-53). */
    @Test
    fun nfr53_type_list_close() {
        show()
        compose.onNodeWithText("Wybierz typ bariery").performClick()
        compose.onNode(hasText("Typ bariery") and isFocused()).assertExists()
        compose.onNode(hasText("Wejście i drzwi") and isHeading()).assertExists()
        compose.onNodeWithText("Zamknij").performClick()

        compose.onNodeWithText("Próg").assertDoesNotExist()
        assertEquals(emptyList<ReportEdit>(), edits.filterIsInstance<ReportEdit.Type>())
        compose.onNode(hasText("Wybierz typ bariery") and isFocused(), useUnmergedTree = false).assertExists()
    }

    /** Przy czcionce 200% „Zamknij” jest widoczne bez przewijania listy (NFR-18). */
    @Test
    @Config(fontScale = 2f)
    fun nfr18_type_close_large_font() {
        show()
        compose.onNodeWithText("Wybierz typ bariery").performScrollTo().performClick()
        compose.onNodeWithText("Zamknij").assertIsDisplayed()
    }

    @Test
    fun pw10_error_above_and_at_field() {
        val form = ReportForm(place = A1)
        show(ReportUiState(form = form, errors = listOf(ReportFormError.MISSING_BARRIER_TYPE)))

        val text = "Brakuje typu bariery. Uzupełnij to pole, aby zapisać."
        assertEquals(2, compose.onAllNodes(hasText(text)).fetchSemanticsNodes().size)
    }

    @Test
    fun brg7_missing_floor_shown() {
        show(ReportUiState(form = ReportForm(place = A1), errors = listOf(ReportFormError.MISSING_FLOOR)))

        val text = "Brakuje piętra. Uzupełnij to pole, aby zapisać."
        assertEquals(2, compose.onAllNodes(hasText(text)).fetchSemanticsNodes().size)
    }

    @Test
    fun nfr7_save_failed_retry() {
        show(ReportUiState(form = ReportForm(place = A1), stage = ReportStage.SAVE_FAILED))
        compose.onNodeWithText("Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.").assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").performClick()

        assertEquals(1, saves)
    }

    @Test
    fun nfr50_save_rejected_message() {
        show(ReportUiState(form = ReportForm(place = A1), stage = ReportStage.SAVE_REJECTED))
        val message = "Nie zapisaliśmy zgłoszenia. Zrób zdjęcie jeszcze raz i zapisz ponownie."
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").assertDoesNotExist()
    }

    @Test
    fun us7_4_exit_dialog() {
        show(ReportUiState(form = ReportForm(place = A1), stage = ReportStage.EXIT_ASKED))
        compose.onNodeWithText("Wyjść bez zapisu?").assertIsDisplayed()
        compose.onNodeWithText("Wyjdź bez zapisu").performClick()

        assertEquals(1, exits)
    }

    @Test
    fun us7_3_description_max_2000() {
        show(place = A1.copy(buildingSymbol = null))
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("a".repeat(2001))

        assertEquals(ReportEdit.Description("a".repeat(2000)), edits.last())
    }

    @Test
    fun us7_3_description_and_photo() {
        show()
        compose.onNodeWithText("Nie fotografuj twarzy ani tablic rejestracyjnych.").performScrollTo()
        compose.onNodeWithText("Zrób zdjęcie").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Obowiązkowy przy typie „inna bariera”.").performScrollTo()

        assertEquals(0, compose.onAllNodes(hasText("Zrób nowe zdjęcie")).fetchSemanticsNodes().size)
    }

    @Test
    fun us7_1_photo_preview() {
        val file = folder.newFile("p1.jpg")
        val image = Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888)
        file.outputStream().use { out -> image.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }
        val photo = PendingPhotoDraft(photoId = "p1", path = file.path)
        show(ReportUiState(form = ReportForm(place = A1), photos = listOf(photo)))

        val preview = hasContentDescription("Zrobione zdjęcie 1 z 1")
        compose.waitUntil(timeoutMillis = DECODE_TIMEOUT_MILLIS) {
            compose.onAllNodes(preview).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(preview).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Zrób nowe zdjęcie").performScrollTo().assertIsDisplayed()
    }

    private fun near(name: String?, isMain: Boolean, meters: Double): EntranceDistance =
        EntranceDistance(
            entrance =
                Entrance(
                    id = "e1",
                    buildingSymbol = "A-1",
                    name = name,
                    isMain = isMain,
                    type = null,
                    location = A1.location,
                    marking = EntranceMarking.NO_DATA,
                ),
            meters = meters,
        )
}

private const val JPEG_QUALITY = 90
private const val DECODE_TIMEOUT_MILLIS = 5_000L
