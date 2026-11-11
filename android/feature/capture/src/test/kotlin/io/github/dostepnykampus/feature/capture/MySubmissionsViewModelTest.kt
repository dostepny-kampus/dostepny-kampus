/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.submissions.OwnSubmission
import io.github.dostepnykampus.core.data.submissions.SendState
import io.github.dostepnykampus.core.data.submissions.StatusRefresh
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.SubmissionStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MySubmissionsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val rows = MutableStateFlow<List<OwnSubmission>>(emptyList())
    private val refreshes = mutableListOf<CompletableDeferred<StatusRefresh>>()
    private val viewModel by lazy {
        MySubmissionsViewModel(
            history = { rows },
            statuses = { CompletableDeferred<StatusRefresh>().also(refreshes::add).await() },
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

    @Test
    fun `pw12 ładowanie i pusta lista`() =
        runTest(dispatcher) {
            assertEquals(MySubmissionsUiState.Loading, viewModel.state.value)
            collect()

            assertEquals(MySubmissions(emptyList()), list())
        }

    @Test
    fun `us8_1 typ budynek i piętro`() =
        runTest(dispatcher) {
            rows.value = listOf(row(id = "a", state = SendState.LOCAL))
            collect()

            val item = list().items.single()
            assertEquals(BarrierType.fromCode("TB-1"), item.type)
            assertEquals("A-1", item.buildingSymbol)
            assertEquals(2, item.floorNumber)
            assertEquals(Instant.ofEpochMilli(SAVED_AT), item.savedAt)
        }

    @Test
    fun `us8_1 bez kodu typu`() =
        runTest(dispatcher) {
            rows.value = listOf(row(id = "a", state = SendState.SENT).copy(barrierType = null))
            collect()

            assertEquals(null, list().items.single().type)
        }

    @Test
    fun `us9_1 liczba w telefonie`() =
        runTest(dispatcher) {
            rows.value =
                listOf(
                    row(id = "a", state = SendState.LOCAL),
                    row(id = "b", state = SendState.SENT),
                    row(id = "c", state = SendState.LOCAL),
                )
            collect()

            assertEquals(2, list().waitingCount)
        }

    @Test
    fun `us9_5 powód odmowy`() =
        runTest(dispatcher) {
            rows.value =
                listOf(
                    row(id = "a", state = SendState.REFUSED, code = 403),
                    row(id = "b", state = SendState.REFUSED, code = 422),
                    row(id = "c", state = SendState.SENT),
                    row(id = "d", state = SendState.REFUSED, code = null),
                    row(id = "e", state = SendState.REFUSED, code = 409),
                )
            collect()

            val reasons = list().items.map { it.refusal }
            assertEquals(
                listOf(
                    RefusalReason.NO_PERMISSION,
                    RefusalReason.NOT_ACCEPTED,
                    null,
                    RefusalReason.UNREADABLE,
                    RefusalReason.CHANGED_ON_SERVER,
                ),
                reasons,
            )
        }

    /** Odmowa 400, 404 i 413 to „Serwer nie przyjął zgłoszenia…”; tylko 403 to brak uprawnień (US9.5, BR-26). */
    @Test
    fun `us9_5 inne kody to nieprzyjęte`() =
        runTest(dispatcher) {
            rows.value = listOf(400, 404, 413).map { row(id = "k$it", state = SendState.REFUSED, code = it) }
            collect()

            assertEquals(List(3) { RefusalReason.NOT_ACCEPTED }, list().items.map { it.refusal })
        }

    /** „Czeka na wysłanie” nie liczy zgłoszeń, których serwer nie przyjął (US9.1, US9.5). */
    @Test
    fun `us9_5 odrzucone bez licznika`() =
        runTest(dispatcher) {
            rows.value =
                listOf(
                    row(id = "a", state = SendState.LOCAL),
                    row(id = "b", state = SendState.REFUSED, code = 403),
                    row(id = "c", state = SendState.REFUSED, code = 400),
                )
            collect()

            assertEquals(1, list().waitingCount)
        }

    /** Wiersz: C3 → „Sprawdzamy statusy…”, błąd z „Spróbuj ponownie”, potem statusy, US8.2, NFR-50, PW-10. */
    @Test
    fun `us8_2 sprawdzanie i ponowienie`() =
        runTest(dispatcher) {
            collect()
            assertEquals(StatusCheck.CHECKING, check())

            refreshes.single().complete(StatusRefresh.FAILED)
            runCurrent()
            assertEquals(StatusCheck.FAILED, check())

            viewModel.retry()
            viewModel.retry()
            runCurrent()
            assertEquals(StatusCheck.CHECKING, check())
            refreshes.last().complete(StatusRefresh.OFFLINE)
            runCurrent()
            assertEquals(StatusCheck.DONE, check())
            assertEquals(2, refreshes.size)
        }

    @Test
    fun `us8_2 status z serwera`() =
        runTest(dispatcher) {
            val rejected = row(id = "a", state = SendState.SENT).copy(serverStatus = SubmissionStatus.REJECTED)
            rows.value = listOf(rejected.copy(rejectionReason = "Nieostre"))
            collect()

            val item = list().items.single()
            assertEquals(SubmissionStatus.REJECTED, item.serverStatus)
            assertEquals("Nieostre", item.rejectionReason)
        }

    private fun check(): StatusCheck = (viewModel.state.value as MySubmissionsUiState.Ready).check

    private fun TestScope.collect() {
        backgroundScope.launch { viewModel.state.collect {} }
        runCurrent()
    }

    private fun list(): MySubmissions = (viewModel.state.value as MySubmissionsUiState.Ready).list
}

private fun row(id: String, state: SendState, code: Int? = null) =
    OwnSubmission(
        id = id,
        savedAt = Instant.ofEpochMilli(SAVED_AT),
        barrierType = "TB-1",
        buildingSymbol = "A-1",
        floorNumber = 2,
        state = state,
        refusalCode = code,
    )

private const val SAVED_AT = 1_762_700_000_000L
