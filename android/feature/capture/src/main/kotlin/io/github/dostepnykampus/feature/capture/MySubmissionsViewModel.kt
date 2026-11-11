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
import io.github.dostepnykampus.core.data.submissions.OwnStatuses
import io.github.dostepnykampus.core.data.submissions.OwnSubmission
import io.github.dostepnykampus.core.data.submissions.OwnSubmissions
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
private const val HTTP_FORBIDDEN = 403
private const val HTTP_CONFLICT = 409

/** Powód „Nie wysłano” na C3 (US9.5, BR-26). */
enum class RefusalReason {
    /** Serwer odebrał rolę (403): „Nie masz już uprawnień do tej czynności…”. */
    NO_PERMISSION,

    /** Inna odmowa: „Serwer nie przyjął zgłoszenia. Otwórz je, popraw i zapisz ponownie.” */
    NOT_ACCEPTED,

    /** Odmowa bez kodu: telefon nie odczytał zgłoszenia, np. brakuje pliku zdjęcia. */
    UNREADABLE,

    /** Konflikt wersji (409): ktoś zmienił zgłoszenie na serwerze po rozpoczęciu poprawki (US9.3, BR-10). */
    CHANGED_ON_SERVER,
}

/**
 * Pozycja listy C3 (US8.1).
 *
 * @property id identyfikator zgłoszenia; „Otwórz” przekazuje go do poprawki w C2 (US8.3)
 * @property savedAt chwila ostatniego zapisu w telefonie
 * @property type typ bariery albo `null`, gdy kod nie jest w słowniku
 * @property buildingSymbol budynek; `null` to „poza budynkiem”
 * @property floorNumber numer piętra budynku podstawowego
 * @property state stan wysyłki
 * @property refusal powód przy „Nie wysłano”; `null` w pozostałych stanach
 * @property serverStatus status z serwera po odświeżeniu (US8.2); `null`, gdy telefon go nie zna
 * @property rejectionReason powód odrzucenia od Weryfikatora (BR-16)
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
 * Lista C3 od najnowszego (US8.1).
 *
 * @property items zgłoszenia
 */
@Immutable
data class MySubmissions(val items: List<OwnSubmissionItem>) {
    /** Liczba zgłoszeń „W telefonie” do napisu „Czeka na wysłanie: N” (US9.1). */
    val waitingCount: Int get() = items.count { it.state == SendState.LOCAL }
}

/** Sprawdzanie statusów z serwera na C3 (US8.2, US9.1, NFR-50). */
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
     */
    data class Ready(val list: MySubmissions, val check: StatusCheck = StatusCheck.DONE) : MySubmissionsUiState
}

/**
 * C3 „Moje zgłoszenia”: własne zgłoszenia z historii w telefonie, więc lista działa bez sieci (US8.1, US9.1, NFR-5).
 * Przy otwarciu i na „Spróbuj ponownie” sprawdza statusy z serwera (US8.2); brak sieci zostawia ostatnie statusy.
 */
@HiltViewModel
class MySubmissionsViewModel
    @Inject
    constructor(history: OwnSubmissions, private val statuses: OwnStatuses) :
    ViewModel() {
        private val check = MutableStateFlow(StatusCheck.CHECKING)

        /** Lista od najnowszego; zmienia się po każdym zapisie, wysyłce z kolejki i sprawdzeniu statusów. */
        val state: StateFlow<MySubmissionsUiState> =
            combine(history.observe(), check) { rows, current ->
                MySubmissionsUiState.Ready(MySubmissions(rows.map { it.toItem() }), current)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MySubmissionsUiState.Loading)

        init {
            refresh()
        }

        /** „Spróbuj ponownie” po błędzie sprawdzania statusów (NFR-50, PW-10). */
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
        refusal = refusalOf(this),
        serverStatus = serverStatus,
        rejectionReason = rejectionReason,
    )

private fun refusalOf(row: OwnSubmission): RefusalReason? =
    when {
        row.state != SendState.REFUSED -> null
        row.refusalCode == null -> RefusalReason.UNREADABLE
        row.refusalCode == HTTP_FORBIDDEN -> RefusalReason.NO_PERMISSION
        row.refusalCode == HTTP_CONFLICT -> RefusalReason.CHANGED_ON_SERVER
        else -> RefusalReason.NOT_ACCEPTED
    }
