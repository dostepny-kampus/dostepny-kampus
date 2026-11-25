/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.conflict

import io.github.dostepnykampus.core.data.submission.conflict.VersionConflict
import io.github.dostepnykampus.core.data.submission.conflict.VersionConflicts
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionField
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import io.github.dostepnykampus.core.designsystem.R as DesignR

/** C4 wczytuje obie wersje i wykonuje wybór raz. Wymagania: US9.3, BR-10 */
@OptIn(ExperimentalCoroutinesApi::class)
class VersionConflictViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val calls = mutableListOf<String>()
    private var conflict: VersionConflict? = conflictOf(SubmissionStatus.SENT)
    private val conflicts =
        object : VersionConflicts {
            override suspend fun find(id: String): VersionConflict? = conflict?.takeIf { it.id == id }

            override suspend fun keepMine(id: String) {
                calls += "moja $id"
            }

            override suspend fun takeServer(id: String) {
                calls += "serwer $id"
            }
        }
    private val viewModel = VersionConflictViewModel(conflicts)

    @BeforeEach
    fun main() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsDifferences() =
        runTest(dispatcher) {
            viewModel.open(ID)
            runCurrent()

            val ready = viewModel.state.value as ConflictUiState.Ready
            assertEquals(listOf(DesignR.string.pole_uciazliwosc), ready.differences, "nazwa pola różnicy")
            assertFalse(ready.isAccepted, "zgłoszenie nie jest przyjęte")
        }

    @Test
    fun acceptedGetsSecondLine() =
        runTest(dispatcher) {
            conflict = conflictOf(SubmissionStatus.ACCEPTED)
            viewModel.open(ID)
            runCurrent()

            assertTrue((viewModel.state.value as ConflictUiState.Ready).isAccepted, "zgłoszenie jest przyjęte")
        }

    @Test
    fun keepMineOnce() =
        runTest(dispatcher) {
            viewModel.open(ID)
            runCurrent()
            viewModel.keepMine()
            viewModel.keepMine()
            viewModel.takeServer()
            runCurrent()

            assertEquals(listOf("moja $ID"), calls, "wybór idzie raz")
            assertEquals(ConflictUiState.Closed, viewModel.state.value, "ekran wraca do C3")
        }

    @Test
    fun takeServerCloses() =
        runTest(dispatcher) {
            viewModel.open(ID)
            runCurrent()
            viewModel.takeServer()
            runCurrent()

            assertEquals(listOf("serwer $ID"), calls, "wersja z serwera")
            assertEquals(ConflictUiState.Closed, viewModel.state.value, "ekran wraca do C3")
        }

    @Test
    fun laterChangesNothing() =
        runTest(dispatcher) {
            viewModel.open(ID)
            runCurrent()
            viewModel.later()
            runCurrent()

            assertTrue(calls.isEmpty(), "odłożony wybór niczego nie zmienia")
            assertEquals(ConflictUiState.Closed, viewModel.state.value, "ekran wraca do C3")
        }

    @Test
    fun missingWithoutConflict() =
        runTest(dispatcher) {
            conflict = null
            viewModel.open(ID)
            runCurrent()

            assertEquals(ConflictUiState.Missing, viewModel.state.value, "bez konfliktu ekran wraca do C3")
        }

    private companion object {
        const val ID = "s-c4"

        fun conflictOf(status: SubmissionStatus): VersionConflict {
            val own =
                SubmissionContent(
                    kind = SubmissionKind.PRECISE,
                    location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
                    photoIds = listOf("p1"),
                    severity = 2,
                )
            val server =
                Submission(
                    id = ID,
                    version = 7,
                    status = status,
                    createdAt = Instant.parse("2026-11-20T10:00:00Z"),
                    content = own.copy(severity = 3),
                )
            return VersionConflict(id = ID, own = own, server = server, differences = listOf(SubmissionField.SEVERITY))
        }
    }
}
