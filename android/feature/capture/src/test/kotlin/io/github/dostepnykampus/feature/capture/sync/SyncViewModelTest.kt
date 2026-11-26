/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.sync

import io.github.dostepnykampus.core.data.submission.sync.SyncNow
import io.github.dostepnykampus.core.data.submission.sync.SyncProgress
import io.github.dostepnykampus.core.data.submission.sync.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

/** Model C4 bez konfliktu: stany kolejki i „Synchronizuj teraz”. Wymagania: US9.1, US9.2, US9.4, NFR-7 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val status = MutableStateFlow(waiting(submissions = 2))
    private val progress = MutableStateFlow<SyncProgress?>(null)
    private val online = MutableStateFlow(true)
    private val expired = MutableStateFlow(false)
    private var isLowSpace = false
    private var syncCalls = 0
    private val sync =
        object : SyncNow {
            override fun progress(): Flow<SyncProgress?> = progress

            override suspend fun syncNow() {
                syncCalls++
            }
        }
    private val viewModel by lazy {
        SyncViewModel(
            status = { status },
            sync = sync,
            network = { online },
            login = { expired },
            space = { isLowSpace },
        )
    }

    @BeforeEach
    fun main() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    /** Przed pierwszym odczytem kolejki ekran czeka. Wymagania: US9.1 */
    @Test
    fun startsLoading() {
        assertEquals(SyncUiState.Loading, viewModel.state.value, "stan przed odczytem")
    }

    /** Kolejka z liczbą czekających i ostatnią synchronizacją. Wymagania: US9.1, NFR-10 */
    @Test
    fun showsQueue() =
        runTest(dispatcher) {
            val ready = readyState()

            assertEquals(SyncPhase.Waiting, ready.phase, "zwykły stan kolejki")
            assertEquals(2, ready.status.waiting, "liczba czekających")
        }

    /** Ostrzeżenie o miejscu stoi obok stanu kolejki. Wymagania: NFR-7 */
    @Test
    fun warnsAboutSpace() =
        runTest(dispatcher) {
            isLowSpace = true

            assertTrue(readyState().isLowSpace, "ostrzeżenie o mniej niż 100 MB")
        }

    /** Stan idzie za postępem przebiegu. Wymagania: US9.1 */
    @Test
    fun followsProgress() =
        runTest(dispatcher) {
            readyState()
            progress.value = SyncProgress(sent = 2, total = 5)
            runCurrent()

            val phase = (viewModel.state.value as SyncUiState.Ready).phase
            assertEquals(SyncPhase.Sending(SyncProgress(sent = 2, total = 5)), phase, "„Wysyłamy 2 z 5…”")
        }

    /** „Synchronizuj teraz” rusza wysyłkę. Wymagania: US9.2 */
    @Test
    fun syncsNow() =
        runTest(dispatcher) {
            viewModel.syncNow()
            runCurrent()

            assertEquals(1, syncCalls, "jedno wywołanie wysyłki")
        }

    /** Wygasłe zalogowanie ma pierwszeństwo przed pozostałymi stanami. Wymagania: US9.4, BR-26 */
    @Test
    fun loginExpiredFirst() {
        val phase =
            syncPhaseOf(
                status = waiting(submissions = 1, isLastRunFailed = true),
                progress = SyncProgress(sent = 0, total = 1),
                isOnline = false,
                isLoginExpired = true,
            )

        assertEquals(SyncPhase.LoginExpired, phase, "wygasłe zalogowanie")
    }

    /** Pusta kolejka to „Wszystko wysłane”, także bez sieci. Wymagania: US9.1 */
    @Test
    fun emptyIsDone() {
        val phase = syncPhaseOf(status = waiting(), progress = null, isOnline = false, isLoginExpired = false)

        assertEquals(SyncPhase.Done, phase, "nic nie czeka")
    }

    /** Bez sieci kolejka czeka na internet. Wymagania: US9.1, US9.2 */
    @Test
    fun offlineWaits() {
        val phase =
            syncPhaseOf(
                status = waiting(submissions = 1, isLastRunFailed = true),
                progress = null,
                isOnline = false,
                isLoginExpired = false,
            )

        assertEquals(SyncPhase.Offline, phase, "brak sieci przed błędem serwera")
    }

    /** Przerwana wysyłka przy sieci to błąd serwera. Wymagania: NFR-50, US9.4 */
    @Test
    fun failedRunIsServerDown() {
        val phase =
            syncPhaseOf(
                status = waiting(markers = 1, isLastRunFailed = true),
                progress = null,
                isOnline = true,
                isLoginExpired = false,
            )

        assertEquals(SyncPhase.ServerDown, phase, "serwer chwilowo nie odpowiada")
    }

    private fun TestScope.readyState(): SyncUiState.Ready {
        backgroundScope.launch { viewModel.state.collect {} }
        runCurrent()
        return viewModel.state.value as SyncUiState.Ready
    }

    private fun waiting(submissions: Int = 0, markers: Int = 0, isLastRunFailed: Boolean = false) =
        SyncStatus(
            submissions = submissions,
            markers = markers,
            lastSyncedAt = Instant.parse("2026-11-12T09:42:00Z"),
            isLastRunFailed = isLastRunFailed,
        )
}
