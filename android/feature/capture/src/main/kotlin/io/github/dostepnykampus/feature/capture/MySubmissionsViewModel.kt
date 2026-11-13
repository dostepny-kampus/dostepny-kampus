/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.data.submissions.OwnStatuses
import io.github.dostepnykampus.core.data.submissions.OwnSubmission
import io.github.dostepnykampus.core.data.submissions.OwnSubmissions
import io.github.dostepnykampus.core.data.submissions.RefusalReason
import io.github.dostepnykampus.core.data.submissions.SendState
import io.github.dostepnykampus.core.data.submissions.StatusRefresh
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.SubmissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Pozycja listy C3.
 *
 * Wymagania: US8.1, US8.3, US8.2, BR-16
 *
 * @property id identyfikator zgłoszenia; „Otwórz” przekazuje go do poprawki w C2
 * @property savedAt chwila ostatniego zapisu w telefonie
 * @property type typ bariery albo `null`, gdy kod nie jest w słowniku
 * @property buildingSymbol budynek; `null` to „poza budynkiem”
 * @property floorNumber numer piętra budynku podstawowego
 * @property state stan wysyłki
 * @property refusal powód przy „Nie wysłano”; `null` w pozostałych stanach
 * @property serverStatus status z serwera po odświeżeniu; `null`, gdy telefon go nie zna
 * @property rejectionReason powód odrzucenia od Weryfikatora
 */
data class OwnSubmissionItem(
    val id: String,
    val savedAt: Instant,
    val type: BarrierType?,
    val buildingSymbol: String?,
    val floorNumber: Int?,
    val state: SendState,
    val refusal: RefusalReason?,
    val serverStatus: SubmissionStatus? = null,
    val rejectionReason: String? = null,
)

/**
 * Lista C3 od najnowszego.
 *
 * Wymagania: US8.1
 *
 * @property items zgłoszenia
 */
@Immutable
data class MySubmissions(val items: List<OwnSubmissionItem>) {
    /** Liczba zgłoszeń „W telefonie” do napisu „Czeka na wysłanie: N”. Wymagania: US9.1 */
    val waitingCount: Int get() = items.count { it.state == SendState.LOCAL }
}

/** Sprawdzanie statusów z serwera na C3. Wymagania: US8.2, US9.1, NFR-50 */
enum class StatusCheck {
    /** „Sprawdzamy statusy…” obok listy z telefonu. */
    CHECKING,

    /** Statusy sprawdzone albo brak sieci: lista ze statusami z ostatniej synchronizacji. */
    DONE,

    /** „Nie udało się sprawdzić statusów…” z „Spróbuj ponownie” (PW-10). */
    FAILED,
}

/** Stan ekranu C3. */
sealed interface MySubmissionsUiState {
    /** Pierwszy odczyt z bazy telefonu trwa. */
    data object Loading : MySubmissionsUiState

    /**
     * Lista z telefonu; pusta pokazuje „Nie masz jeszcze zgłoszeń…” (PW-12).
     *
     * @property list zgłoszenia
     * @property check sprawdzanie statusów z serwera
     * @property isOnline czy telefon ma połączenie z internetem
     */
    data class Ready(
        val list: MySubmissions,
        val check: StatusCheck = StatusCheck.DONE,
        val isOnline: Boolean = true,
    ) : MySubmissionsUiState
}

/**
 * C3 „Moje zgłoszenia”: własne zgłoszenia z historii w telefonie, więc lista działa bez sieci.
 * Przy otwarciu i na „Spróbuj ponownie” sprawdza statusy z serwera; brak sieci zostawia ostatnie statusy.
 *
 * Wymagania: US8.1, US9.1, NFR-5, US8.2
 */
@HiltViewModel
class MySubmissionsViewModel
    @Inject
    constructor(
        history: OwnSubmissions,
        private val statuses: OwnStatuses,
        networkStatus: NetworkStatus,
    ) : ViewModel() {
        private val check = MutableStateFlow(StatusCheck.CHECKING)

        /** Lista od najnowszego; zmienia się po każdym zapisie, wysyłce z kolejki i sprawdzeniu statusów. */
        val state: StateFlow<MySubmissionsUiState> =
            combine(history.observe(), check, networkStatus.isOnline()) { rows, current, online ->
                MySubmissionsUiState.Ready(MySubmissions(rows.map { it.toItem() }), current, isOnline = online)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MySubmissionsUiState.Loading)

        init {
            refresh()
        }

        /** „Spróbuj ponownie” po błędzie sprawdzania statusów (PW-10). Wymagania: NFR-50 */
        fun retry() {
            if (check.value != StatusCheck.CHECKING) refresh()
        }

        private fun refresh() {
            check.value = StatusCheck.CHECKING
            viewModelScope.launch {
                check.value = if (statuses.refresh() == StatusRefresh.FAILED) StatusCheck.FAILED else StatusCheck.DONE
            }
        }
    }

internal fun OwnSubmission.toItem(): OwnSubmissionItem =
    OwnSubmissionItem(
        id = id,
        savedAt = savedAt,
        type = barrierType?.let { BarrierType.fromCode(it) },
        buildingSymbol = buildingSymbol,
        floorNumber = floorNumber,
        state = state,
        refusal = refusal,
        serverStatus = serverStatus,
        rejectionReason = rejectionReason,
    )
