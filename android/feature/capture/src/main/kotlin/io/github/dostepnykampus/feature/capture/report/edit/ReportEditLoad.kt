/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.submission.pending.PendingEdit
import io.github.dostepnykampus.core.data.submission.pending.PendingEdits
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.feature.capture.navigation.ReportEditRoute
import io.github.dostepnykampus.feature.capture.report.form.ReportFloor
import io.github.dostepnykampus.feature.capture.report.form.ReportForm
import io.github.dostepnykampus.feature.capture.report.form.ReportPlace
import io.github.dostepnykampus.feature.capture.report.form.ReportUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Wczytanie poprawki z kolejki telefonu dla C2. Wymagania: US8.3 */
sealed interface ReportEditLoad {
    /** Odczyt z bazy trwa. */
    data object Loading : ReportEditLoad

    /**
     * Pola i zdjęcia zapisanego zgłoszenia.
     *
     * @property start stan formularza na start poprawki
     */
    data class Ready(val start: ReportUiState) : ReportEditLoad

    /** Zgłoszenia nie ma już w telefonie, np. wysłało się w międzyczasie; ekran wraca do C3. */
    data object Missing : ReportEditLoad
}

/**
 * Model widoku wczytujący raz na ekran zgłoszenie z kolejki, zgłoszenie przyjęte albo nieaktualną zmianę do ponownego
 * wysłania.
 *
 * Wymagania: US8.3, BR-18, BR-23
 */
@HiltViewModel
class ReportEditViewModel
    @Inject
    constructor(
        private val edits: PendingEdits,
        private val barriers: AcceptedBarriers,
    ) : ViewModel() {
        private val state = MutableStateFlow<ReportEditLoad>(ReportEditLoad.Loading)
        private var requested: String? = null

        /** Wynik wczytania. */
        val load: StateFlow<ReportEditLoad> = state.asStateFlow()

        /** Wariant dla testów poprawki z kolejki bez źródła zgłoszeń przyjętych. */
        constructor(edits: PendingEdits) : this(edits = edits, barriers = AcceptedBarriers { emptyList() })

        /** Wczytuje zgłoszenie z [route]; kolejne wywołania nic nie zmieniają, więc obrót ekranu nie kasuje pól. */
        fun open(route: ReportEditRoute) {
            if (requested != null) return
            val id = route.submissionId
            requested = id
            viewModelScope.launch {
                val loaded =
                    when {
                        route.isAccepted -> barriers.barriers().firstOrNull { it.id == id }?.toReplaceUiState()
                        route.isResend -> edits.load(id)?.let { old -> resendOf(old) }
                        else -> edits.load(id)?.toReportUiState()
                    }
                state.value = if (loaded == null) ReportEditLoad.Missing else ReportEditLoad.Ready(loaded)
            }
        }

        private suspend fun resendOf(old: PendingEdit): ReportUiState? {
            val replaced = old.content.replacesSubmissionId ?: return null
            val current = barriers.barriers().firstOrNull { it.replacesSubmissionId == replaced }
            return current?.let { old.toResendUiState(currentId = it.id) }
        }
    }

/** Stan formularza z zapisanej treści; piętro poza budynkiem i nieznane kody pomija. Wymagania: BR-G4, US8.3 */
internal fun PendingEdit.toReportUiState(): ReportUiState {
    val place =
        ReportPlace(
            location = content.location,
            accuracyMeters = content.accuracyMeters ?: 0.0,
            buildingSymbol = content.buildingSymbol,
            isManualCorrection = content.isManualCorrection == true,
        )
    val form =
        ReportForm(
            place = place,
            floor = content.floor?.toReportFloor()?.takeIf { place.buildingSymbol != null },
            barrierType = content.barrierType?.let(BarrierType::fromCode),
            severity = content.severity?.let(Severity::fromValue),
            photoIds = content.photoIds,
            description = content.description.orEmpty(),
            replacesSubmissionId = content.replacesSubmissionId,
            editedSubmission = edited,
        )
    return ReportUiState(form = form, photos = photos)
}

/**
 * Stan formularza na start ponownego wysłania nieaktualnej zmiany: treść i zdjęcia tej zmiany jako nowa zmiana
 * bieżącego przyjętego zgłoszenia [currentId]; zapis tworzy nowe zgłoszenie, a nieaktualna zmiana zostaje odrzucona.
 *
 * Wymagania: US8.3, BR-18, BR-23
 */
internal fun PendingEdit.toResendUiState(currentId: String): ReportUiState {
    val start = toReportUiState()
    val form = start.form.copy(replacesSubmissionId = currentId, editedSubmission = null)
    return start.copy(form = form)
}

internal fun SubmissionFloor.toReportFloor(): ReportFloor? =
    floorId?.let { ReportFloor.Planned(it) }
        ?: number?.let { ReportFloor.Numbered(number = it, isMezzanine = isMezzanine == true) }

/**
 * Stan formularza na start zmiany zgłoszenia przyjętego; zapis tworzy nowe zgłoszenie z powiązaniem.
 *
 * Wymagania: BR-18
 */
internal fun AcceptedBarrier.toReplaceUiState(): ReportUiState {
    val place =
        ReportPlace(
            location = location,
            accuracyMeters = 0.0,
            buildingSymbol = buildingSymbol,
            isManualCorrection = false,
        )
    val form =
        ReportForm(
            place = place,
            floor = floor?.toReportFloor()?.takeIf { place.buildingSymbol != null },
            barrierType = type,
            severity = severity,
            description = description.orEmpty(),
            replacesSubmissionId = id,
        )
    return ReportUiState(form = form)
}
