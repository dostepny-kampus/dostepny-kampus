/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.edit

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.submission.pending.PendingEdit
import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.feature.capture.navigation.ReportEditMode
import io.github.dostepnykampus.feature.capture.navigation.ReportEditRoute
import io.github.dostepnykampus.feature.capture.report.form.edited
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Poprawka z C3 wczytuje zgłoszenie z kolejki raz na ekran, a ponowne wysłanie nieaktualnej zmiany wczytuje ją jako
 * nową zmianę bieżącego przyjętego zgłoszenia.
 *
 * Wymagania: US8.3, BR-18, BR-23
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportEditViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val asked = mutableListOf<String>()
    private val saved =
        PendingEdit(
            edited = EditedSubmission("s1", baseVersion = 1),
            content =
                SubmissionContent(
                    kind = SubmissionKind.PRECISE,
                    location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
                    photoIds = listOf("p1"),
                ),
            photos = listOf(PendingPhotoDraft(photoId = "p1", path = "/data/p1.jpg")),
        )
    private val viewModel =
        ReportEditViewModel { id ->
            asked.add(id)
            saved.takeIf { it.edited.id == id }
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
    fun loadsOnce() =
        runTest(dispatcher) {
            assertEquals(ReportEditLoad.Loading, viewModel.load.value)
            viewModel.open(ReportEditRoute("s1"))
            viewModel.open(ReportEditRoute("s1"))
            runCurrent()

            assertEquals(ReportEditLoad.Ready(saved.toReportUiState()), viewModel.load.value)
            assertEquals(listOf("s1"), asked)
        }

    @Test
    fun reportsMissingOnPhone() =
        runTest(dispatcher) {
            viewModel.open(ReportEditRoute("s2"))
            runCurrent()

            assertEquals(ReportEditLoad.Missing, viewModel.load.value)
        }

    @Test
    fun resendsAsNewChange() =
        runTest(dispatcher) {
            val outdated = saved.copy(content = saved.content.copy(replacesSubmissionId = "a0"))
            val barriers = listOf(accepted(id = "a9", replaced = null), accepted(id = "a1", replaced = "a0"))
            val resend = ReportEditViewModel(edits = { outdated }, barriers = { barriers })
            resend.open(ReportEditRoute("s1", ReportEditMode.RESEND_OUTDATED))
            runCurrent()

            val form = (resend.load.value as ReportEditLoad.Ready).start.form
            assertEquals("a1", form.replacesSubmissionId, "zmiana bieżącego przyjętego zgłoszenia")
            assertNull(form.editedSubmission, "zapis tworzy nowe zgłoszenie zamiast poprawki starej zmiany")
            assertEquals(outdated.content.photoIds, form.photoIds, "zdjęcia starej zmiany")
        }

    @Test
    fun waitsForCurrentAccepted() =
        runTest(dispatcher) {
            val outdated = saved.copy(content = saved.content.copy(replacesSubmissionId = "a0"))
            val resend = ReportEditViewModel(edits = { outdated }, barriers = AcceptedBarriers { emptyList() })
            resend.open(ReportEditRoute("s1", ReportEditMode.RESEND_OUTDATED))
            runCurrent()

            assertEquals(ReportEditLoad.Missing, resend.load.value, "bez bieżącego przyjętego wraca do C3")
        }

    private fun accepted(id: String, replaced: String?) =
        AcceptedBarrier(
            id = id,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            severity = Severity.BLOCKING,
            type = null,
            buildingSymbol = null,
            verifiedAt = null,
            replacesSubmissionId = replaced,
        )
}
