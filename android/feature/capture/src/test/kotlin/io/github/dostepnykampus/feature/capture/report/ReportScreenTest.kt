/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.geo.building.EntranceDistance
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.capture.photo.LocalPhotoDecodeContext
import io.github.dostepnykampus.feature.capture.photo.PhotoCamera
import io.github.dostepnykampus.feature.capture.report.form.PlanFloor
import io.github.dostepnykampus.feature.capture.report.form.PlanFloors
import io.github.dostepnykampus.feature.capture.report.form.ReportEdit
import io.github.dostepnykampus.feature.capture.report.form.ReportFloor
import io.github.dostepnykampus.feature.capture.report.form.ReportForm
import io.github.dostepnykampus.feature.capture.report.form.ReportFormError
import io.github.dostepnykampus.feature.capture.report.form.ReportPlace
import io.github.dostepnykampus.feature.capture.report.form.ReportStage
import io.github.dostepnykampus.feature.capture.report.form.ReportUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Ekran C2: miejsce z C1, piętro liczbą, typ bariery z listy, uciążliwość i „Zapisz”.
 *
 * Wymagania: US7.1, US7.2, US7.3, US7.4, US8.3, BR-7, BR-10, BR-18, BR-G4, NFR-7, NFR-50
 */
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
        compose.setContent {
            DostepnyKampusTheme {
                CompositionLocalProvider(LocalPhotoDecodeContext provides EmptyCoroutineContext) {
                    ReportContent(state = state, actions = actions)
                }
            }
        }
    }

    @Test
    fun showsPlaceWithoutCoordinates() {
        show()

        compose.onNodeWithText("Opisz barierę").assertIsDisplayed()
        compose.onNodeWithText("Budynek A-1").assertIsDisplayed()
        compose.onNodeWithText("dokładność 0,8 m").assertIsDisplayed()
    }

    @Test
    fun describesPlaceInWords() {
        show(A1.copy(buildingName = "Gmach Główny", nearestEntrance = near(name = null, isMain = true, meters = 3.2)))

        compose.onNodeWithText("Budynek A-1 Gmach Główny").assertIsDisplayed()
        compose.onNodeWithText("ok. 3 m od wejścia głównego, dokładność 0,8 m").assertIsDisplayed()
    }

    @Test
    fun namesEntrance() {
        show(A1.copy(nearestEntrance = near(name = "Wejście Cesarskie", isMain = false, meters = 0.3)))

        compose.onNodeWithText("ok. 1 m od wejścia „Wejście Cesarskie”, dokładność 0,8 m").assertIsDisplayed()
    }

    @Test
    fun showsEditHeader() {
        show(replaces = "s-old")

        compose.onNodeWithText("Popraw zgłoszenie").assertIsDisplayed()
    }

    @Test
    fun showsCorrectionHeader() {
        val edited = EditedSubmission(id = "s1", baseVersion = 2)
        show(ReportUiState(form = ReportForm(place = A1, editedSubmission = edited)))

        compose.onNodeWithText("Popraw zgłoszenie").assertIsDisplayed()
    }

    /**
     * Miejsce na ulicy: formularz nie pokazuje pola piętra.
     * Scenariusz (przykład: poza budynek, na ulicę): pomiar-precyzyjny.feature: Piętro zostaje po zmianie miejsca
     *   tylko w tym samym budynku
     *
     * Wymagania: BR-G4
     */
    @Test
    fun hidesFloorOutside() {
        show(place = A1.copy(buildingSymbol = null))

        compose.onNodeWithText("Poza budynkiem").assertIsDisplayed()
        assertEquals(0, compose.onAllNodes(hasText("Piętro")).fetchSemanticsNodes().size)
    }

    @Test
    fun savesWithSeverity() {
        show()
        compose.onNodeWithText("Blokada").performScrollTo().performClick()
        compose.onNodeWithText("Zapisz").performClick()

        assertEquals(ReportEdit.Rating(Severity.BLOCKING), edits.last())
        assertEquals(1, saves)
    }

    @Test
    fun acceptsFloorNumber() {
        show()
        compose.onNodeWithText("Numer piętra").performTextInput("-1")

        assertEquals(ReportEdit.Floor(ReportFloor.Numbered(number = -1, isMezzanine = false)), edits.last())
    }

    @Test
    fun rejectsNonIntegerFloor() {
        show()
        compose.onNodeWithText("Numer piętra").performTextInput("1,5")

        assertEquals(ReportEdit.Floor(null), edits.last())
        compose.onNodeWithText("Wpisz liczbę całkowitą, np. 0 albo -1.").assertIsDisplayed()
    }

    @Test
    fun clearsFloorForNewBuilding() {
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
    fun takesFloorFromPlan() {
        val floors = listOf(PlanFloor(id = "A-1-p", label = "Piwnica"), PlanFloor(id = "A-1-3", label = "3 piętro"))
        show(A1.copy(floors = PlanFloors(floors, source = "Politechnika Wrocławska")))

        compose.onNodeWithText("Źródło: Politechnika Wrocławska.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("3 piętro").performScrollTo().performClick()

        assertEquals(ReportEdit.Floor(ReportFloor.Planned("A-1-3")), edits.last())
        assertEquals(0, compose.onAllNodes(hasText("Numer piętra")).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodes(hasText("Nie mamy planu", substring = true)).fetchSemanticsNodes().size)
    }

    /**
     * Budynek szczegółowy: pięć etykiet pięter z planu i brak pola na piętro wpisane ręcznie.
     * Scenariusz: pomiar-precyzyjny.feature: Piętro w budynku szczegółowym wybiera się z etykiet z danych PWr
     *
     * Wymagania: US7.2
     */
    @Test
    fun offersOnlyPlanFloorLabels() {
        val labels = listOf("Piwnica", "Parter", "1 piętro", "2 piętro", "3 piętro")
        val floors = labels.mapIndexed { index, label -> PlanFloor(id = "A-1-$index", label = label) }
        show(A1.copy(floors = PlanFloors(floors, source = "Politechnika Wrocławska")))

        labels.forEach { label -> compose.onNodeWithText(label).performScrollTo().assertIsDisplayed() }
        assertEquals(
            "brak pola na piętro wpisane ręcznie",
            0,
            compose.onAllNodes(hasText("Numer piętra")).fetchSemanticsNodes().size,
        )
    }

    /**
     * Budynek szczegółowy: piętra we wspólnym wyborze piętra, wybrane widać w stanie opcji.
     *
     * Wymagania: US7.2, NFR-18
     */
    @Test
    fun usesSharedFloorPicker() {
        val floors = listOf(PlanFloor(id = "A-1-p", label = "Piwnica"), PlanFloor(id = "A-1-3", label = "3 piętro"))
        val place = A1.copy(floors = PlanFloors(floors, source = "Politechnika Wrocławska"))
        show(ReportUiState(form = ReportForm(place = place, floor = ReportFloor.Planned("A-1-3"))))

        val group = SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)
        compose.onNode(group and hasAnyChild(hasText("Piwnica"))).assertExists()
        compose.onNode(hasText("3 piętro") and isSelected()).performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("Piwnica") and isNotSelected()).performScrollTo().performClick()

        assertEquals(ReportEdit.Floor(ReportFloor.Planned("A-1-p")), edits.last())
    }

    @Test
    fun choosesTypeFromList() {
        show()
        compose.onNodeWithText("Wybierz typ bariery").performClick()
        compose.onNodeWithText("Próg").performScrollTo().performClick()

        assertEquals(ReportEdit.Type(BarrierType.THRESHOLD), edits.last())
    }

    /** Lista typów: „Zamknij” zamyka ją bez zmiany typu, a fokus wraca na pole. Wymagania: NFR-53 */
    @Test
    fun closesTypeList() {
        show()
        compose.onNodeWithText("Wybierz typ bariery").performClick()
        compose.onNode(hasText("Typ bariery") and isFocused()).assertExists()
        compose.onNode(hasText("Wejście i drzwi") and isHeading()).assertExists()
        compose.onNodeWithText("Zamknij").performClick()

        compose.onNodeWithText("Próg").assertDoesNotExist()
        assertEquals(emptyList<ReportEdit>(), edits.filterIsInstance<ReportEdit.Type>())
        compose.onNode(hasText("Wybierz typ bariery") and isFocused(), useUnmergedTree = false).assertExists()
    }

    /** Przy czcionce 200% „Zamknij” jest widoczne bez przewijania listy. Wymagania: NFR-18 */
    @Test
    @Config(fontScale = 2f)
    fun showsCloseAtLargeFont() {
        show()
        compose.onNodeWithText("Wybierz typ bariery").performScrollTo().performClick()
        compose.onNodeWithText("Zamknij").assertIsDisplayed()
    }

    @Test
    fun showsErrorAboveAndAtField() {
        val form = ReportForm(place = A1)
        show(ReportUiState(form = form, errors = listOf(ReportFormError.MISSING_BARRIER_TYPE)))

        val text = "Brakuje typu bariery. Uzupełnij to pole, aby zapisać."
        assertEquals(2, compose.onAllNodes(hasText(text)).fetchSemanticsNodes().size)
    }

    @Test
    fun showsMissingFloor() {
        show(ReportUiState(form = ReportForm(place = A1), errors = listOf(ReportFormError.MISSING_FLOOR)))

        val text = "Brakuje piętra. Uzupełnij to pole, aby zapisać."
        assertEquals(2, compose.onAllNodes(hasText(text)).fetchSemanticsNodes().size)
    }

    /**
     * Brak opisu jest nazwany w podsumowaniu i przy polu, gdy wybrano TB-29 (PW-10).
     * Scenariusz (częściowo): pomiar-precyzyjny.feature: Typ TB-29 „inna bariera” wymaga opisu
     *
     * Wymagania: BR-7
     */
    @Test
    fun showsMissingDescription() {
        show(
            ReportUiState(
                form = ReportForm(place = A1, barrierType = BarrierType.OTHER_BARRIER),
                errors = listOf(ReportFormError.MISSING_DESCRIPTION),
            ),
        )

        val text = "Brakuje opisu. Przy typie „inna bariera” opis jest obowiązkowy."
        assertEquals(2, compose.onAllNodes(hasText(text)).fetchSemanticsNodes().size)
    }

    @Test
    fun offersRetryAfterFailedSave() {
        show(ReportUiState(form = ReportForm(place = A1), stage = ReportStage.SAVE_FAILED))
        compose.onNodeWithText("Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.").assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").performClick()

        assertEquals(1, saves)
    }

    @Test
    fun showsRejectedSaveMessage() {
        show(ReportUiState(form = ReportForm(place = A1), stage = ReportStage.SAVE_REJECTED))
        val message = "Nie zapisaliśmy zgłoszenia. Zrób zdjęcie jeszcze raz i zapisz ponownie."
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").assertDoesNotExist()
    }

    @Test
    fun showsUnknownVersionMessage() {
        show(ReportUiState(form = ReportForm(place = A1), stage = ReportStage.VERSION_UNKNOWN))
        compose.onNodeWithText("Nie zapisaliśmy poprawki", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Zrób zdjęcie jeszcze raz", substring = true).assertDoesNotExist()
    }

    @Test
    fun asksBeforeExit() {
        show(ReportUiState(form = ReportForm(place = A1), stage = ReportStage.EXIT_ASKED))
        compose.onNodeWithText("Wyjść bez zapisu?").assertIsDisplayed()
        compose.onNodeWithText("Wyjdź bez zapisu").performClick()

        assertEquals(1, exits)
    }

    @Test
    fun limitsDescriptionLength() {
        show(place = A1.copy(buildingSymbol = null))
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("a".repeat(2001))

        assertEquals(ReportEdit.Description("a".repeat(2000)), edits.last())
    }

    @Test
    fun showsDescriptionAndPhoto() {
        show()
        compose.onNodeWithText("Nie fotografuj twarzy ani tablic rejestracyjnych.").performScrollTo()
        compose.onNodeWithText("Zrób zdjęcie").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Obowiązkowy przy typie „inna bariera”.").performScrollTo()

        assertEquals(0, compose.onAllNodes(hasText("Zrób nowe zdjęcie")).fetchSemanticsNodes().size)
    }

    @Test
    fun showsPhotoPreview() {
        val file = folder.newFile("p1.jpg")
        val image = Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888)
        file.outputStream().use { out -> image.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }
        val photo = PendingPhotoDraft(photoId = "p1", path = file.path)
        show(ReportUiState(form = ReportForm(place = A1), photos = listOf(photo)))

        compose.waitForIdle()
        compose.onNode(hasContentDescription("Zrobione zdjęcie 1 z 1")).performScrollTo().assertIsDisplayed()
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
