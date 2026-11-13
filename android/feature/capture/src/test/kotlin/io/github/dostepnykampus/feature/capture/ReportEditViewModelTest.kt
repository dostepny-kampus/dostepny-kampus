/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.submissions.EditedSubmission
import io.github.dostepnykampus.core.data.submissions.PendingEdit
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.feature.capture.quick.content
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

/** Poprawka z C3 wczytuje zgłoszenie z kolejki raz na ekran. Wymagania: US8.3 */
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
    fun `us8_3 wczytuje raz`() =
        runTest(dispatcher) {
            assertEquals(ReportEditLoad.Loading, viewModel.load.value)
            viewModel.open("s1")
            viewModel.open("s1")
            runCurrent()

            assertEquals(ReportEditLoad.Ready(saved.toReportUiState()), viewModel.load.value)
            assertEquals(listOf("s1"), asked)
        }

    @Test
    fun `us8_3 brak w telefonie`() =
        runTest(dispatcher) {
            viewModel.open("s2")
            runCurrent()

            assertEquals(ReportEditLoad.Missing, viewModel.load.value)
        }
}
