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
import io.github.dostepnykampus.core.data.submissions.OwnSubmission
import io.github.dostepnykampus.core.data.submissions.OwnSubmissions
import io.github.dostepnykampus.core.data.submissions.SendState
import io.github.dostepnykampus.core.model.BarrierType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L
private const val HTTP_FORBIDDEN = 403

/** Powód „Nie wysłano” na C3 (US9.5, BR-26). */
enum class RefusalReason {
    /** Serwer odebrał rolę (403): „Nie masz już uprawnień do tej czynności…”. */
    NO_PERMISSION,

    /** Inna odmowa: „Serwer nie przyjął zgłoszenia. Otwórz je, popraw i zapisz ponownie.” */
    NOT_ACCEPTED,
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
 */
data class OwnSubmissionItem(
    val id: String,
    val savedAt: Instant,
    val type: BarrierType?,
    val buildingSymbol: String?,
    val floorNumber: Int?,
    val state: SendState,
    val refusal: RefusalReason?,
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

/** Stan ekranu C3. */
sealed interface MySubmissionsUiState {
    /** Pierwszy odczyt z bazy telefonu trwa. */
    data object Loading : MySubmissionsUiState

    /**
     * Lista z telefonu; pusta pokazuje „Nie masz jeszcze zgłoszeń…” (PW-12).
     *
     * @property list zgłoszenia
     */
    data class Ready(val list: MySubmissions) : MySubmissionsUiState
}

/**
 * C3 „Moje zgłoszenia”: własne zgłoszenia z historii w telefonie, więc lista działa bez sieci (US8.1, US9.1, NFR-5).
 * Statusy z serwera (w weryfikacji, przyjęte, odrzucone) dojdą po logowaniu (US8.2).
 */
@HiltViewModel
class MySubmissionsViewModel
    @Inject
    constructor(history: OwnSubmissions) : ViewModel() {
        /** Lista od najnowszego; zmienia się po każdym zapisie i wysyłce z kolejki. */
        val state: StateFlow<MySubmissionsUiState> =
            history
                .observe()
                .map { rows -> MySubmissionsUiState.Ready(MySubmissions(rows.map { it.toItem() })) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MySubmissionsUiState.Loading)
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
    )

private fun refusalOf(row: OwnSubmission): RefusalReason? =
    when {
        row.state != SendState.REFUSED -> null
        row.refusalCode == HTTP_FORBIDDEN -> RefusalReason.NO_PERMISSION
        else -> RefusalReason.NOT_ACCEPTED
    }
