/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.submissions.EditedSubmission
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.data.submissions.SaveRefusal
import io.github.dostepnykampus.core.data.submissions.SaveRefusedException
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.core.model.SubmissionContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

/** Ekran C2: braki po „Zapisz”, zapis w kolejce, błąd zapisu i pytanie przy wyjściu (US7.1, US7.4, NFR-7). */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportFormViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val saved = mutableListOf<Pair<SubmissionContent, List<PendingPhotoDraft>>>()
    private val editedSaves = mutableListOf<EditedSubmission?>()
    private var failure: Exception? = null
    private var opened = ReportForm(place = A1)
    private val viewModel by lazy {
        ReportFormViewModel { content, photos, edited ->
            failure?.let { throw it }
            saved.add(content to photos)
            editedSaves.add(edited)
        }.apply { open(opened) }
    }

    @BeforeEach
    fun main() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    @Test
    fun `braki dopiero po zapisz`() {
        viewModel.edit(ReportEdit.Type(BarrierType.THRESHOLD))
        assertEquals(emptyList<ReportFormError>(), state().errors)

        viewModel.save()

        assertEquals(
            listOf(ReportFormError.MISSING_FLOOR, ReportFormError.MISSING_SEVERITY, ReportFormError.MISSING_PHOTO),
            state().errors,
        )
        assertEquals(ReportStage.EDITING, state().stage)
        assertEquals(emptyList<Any>(), saved)
    }

    @Test
    fun `zapisz włącza braki na stałe`() {
        viewModel.save()
        fill()
        assertEquals(emptyList<ReportFormError>(), state().errors)

        viewModel.edit(ReportEdit.Type(BarrierType.OTHER_BARRIER))

        assertEquals(listOf(ReportFormError.MISSING_DESCRIPTION), state().errors)
    }

    @Test
    fun `zapis trafia do kolejki`() =
        runTest(dispatcher) {
            fill()
            viewModel.save()
            assertEquals(ReportStage.SAVING, state().stage)
            runCurrent()

            assertEquals(ReportStage.SAVED, state().stage)
            val (content, photos) = saved.single()
            assertEquals("TB-13", content.barrierType)
            assertEquals(listOf("p1"), content.photoIds)
            assertEquals(listOf(PHOTO), photos)
        }

    @Test
    fun `poprawka zapisuje to samo`() =
        runTest(dispatcher) {
            val edited = EditedSubmission(id = "s1", baseVersion = 2)
            opened = ReportForm(place = A1, editedSubmission = edited)
            fill()
            viewModel.save()
            runCurrent()

            assertEquals(listOf(edited), editedSaves)
        }

    @Test
    fun `drugie zapisz nie dubluje`() =
        runTest(dispatcher) {
            fill()
            viewModel.save()
            viewModel.save()
            runCurrent()
            viewModel.save()

            assertEquals(1, saved.size)
        }

    @Test
    fun `wstecz nie odblokowuje zapisz`() =
        runTest(dispatcher) {
            fill()
            viewModel.save()
            viewModel.back()
            viewModel.stay()
            viewModel.confirmExit()
            viewModel.edit(ReportEdit.Description("Próg"))
            viewModel.save()
            assertEquals(ReportStage.SAVING, state().stage)
            assertEquals("", state().form.description)
            runCurrent()

            assertEquals(ReportStage.SAVED, state().stage)
            assertEquals(1, saved.size)
        }

    @Test
    fun `za mało miejsca`() =
        runTest(dispatcher) {
            failure = IOException("brak miejsca")
            fill()
            viewModel.save()
            runCurrent()
            assertEquals(ReportStage.SAVE_FAILED, state().stage)

            failure = null
            viewModel.stay()
            viewModel.save()
            runCurrent()
            assertEquals(ReportStage.SAVED, state().stage)
            assertEquals(1, saved.size)
        }

    /** Kolejka odrzuca wpis: błąd zapisu bez awarii, a wpisane pola zostają (NFR-7, NFR-50). */
    @Test
    fun `odrzucony wpis zostaje`() =
        runTest(dispatcher) {
            failure = SaveRefusedException(SaveRefusal.PHOTOS_MISMATCH, "zdjęcia nie pasują do treści")
            fill()
            val form = state().form
            viewModel.save()
            runCurrent()
            assertEquals(ReportStage.SAVE_REJECTED, state().stage)
            assertEquals(form, state().form)
            assertEquals(listOf(PHOTO), state().photos)
            assertEquals(0, saved.size)
        }

    /** Nieznana wersja poprawki nie prosi o nowe zdjęcie, bo zdjęcie nic by nie zmieniło (BR-10, NFR-50). */
    @Test
    fun `nieznana wersja bez zdjecia`() =
        runTest(dispatcher) {
            failure = SaveRefusedException(SaveRefusal.UNKNOWN_VERSION, "wersja nieznana")
            fill()
            val form = state().form
            viewModel.save()
            runCurrent()
            assertEquals(ReportStage.VERSION_UNKNOWN, state().stage)
            assertEquals(form, state().form)
        }

    /** Anulowanie zapisu nie zmienia stanu i nie jest błędem zapisu. */
    @Test
    fun `anulowanie bez zmiany`() =
        runTest(dispatcher) {
            failure = CancellationException("ekran zamknięty")
            fill()
            viewModel.save()
            runCurrent()
            assertEquals(ReportStage.SAVING, state().stage)
        }

    @Test
    fun `wstecz bez zmian wychodzi`() {
        viewModel.back()

        assertEquals(ReportStage.LEFT, state().stage)
    }

    @Test
    fun `wstecz ze zmianami pyta`() {
        viewModel.edit(ReportEdit.Description("Próg"))
        viewModel.back()
        assertEquals(ReportStage.EXIT_ASKED, state().stage)

        viewModel.stay()
        assertEquals(ReportStage.EDITING, state().stage)
        assertEquals("Próg", state().form.description)

        viewModel.back()
        viewModel.confirmExit()
        assertEquals(ReportStage.LEFT, state().stage)
    }

    /**
     * Wybrany TB-1 bez zapisu: rezygnacja z wyjścia zostawia formularz z TB-1, a potwierdzone wyjście zamyka go bez
     * zapisu w kolejce (US7.4).
     * Scenariusz: pomiar-precyzyjny.feature: Wyjście z formularza z niezapisanymi zmianami wymaga potwierdzenia
     */
    @Test
    fun `wyjście z wybranym typem`() =
        runTest(dispatcher) {
            viewModel.edit(ReportEdit.Type(BarrierType.STAIRS_AT_ENTRANCE))
            viewModel.back()
            assertEquals(ReportStage.EXIT_ASKED, state().stage)

            viewModel.stay()
            assertEquals(ReportStage.EDITING, state().stage)
            assertEquals(BarrierType.STAIRS_AT_ENTRANCE, state().form.barrierType)

            viewModel.back()
            viewModel.confirmExit()
            runCurrent()
            assertEquals(ReportStage.LEFT, state().stage)
            assertEquals(emptyList<Any>(), saved)
        }

    @Test
    fun `drugie otwarcie nie kasuje`() {
        viewModel.edit(ReportEdit.Description("Próg"))
        viewModel.open(ReportForm(place = A1))

        assertEquals("Próg", state().form.description)
    }

    private fun fill() {
        viewModel.edit(ReportEdit.Floor(ReportFloor.Planned("A-1-0")))
        viewModel.edit(ReportEdit.Type(BarrierType.THRESHOLD))
        viewModel.edit(ReportEdit.Rating(Severity.ONLY_WITH_HELP))
        viewModel.edit(ReportEdit.Photo(PHOTO))
    }

    private fun state(): ReportUiState = checkNotNull(viewModel.uiState.value)
}

private val PHOTO = PendingPhotoDraft(photoId = "p1", path = "/data/p1.jpg")
