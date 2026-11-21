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

/** Model widoku wczytujący zgłoszenie z kolejki raz na ekran. Wymagania: US8.3 */
@HiltViewModel
class ReportEditViewModel
    @Inject
    constructor(private val edits: PendingEdits) : ViewModel() {
        private val state = MutableStateFlow<ReportEditLoad>(ReportEditLoad.Loading)
        private var requested: String? = null

        /** Wynik wczytania. */
        val load: StateFlow<ReportEditLoad> = state.asStateFlow()

        /** Wczytuje zgłoszenie [id]; kolejne wywołania nic nie zmieniają, więc obrót ekranu nie kasuje pól. */
        fun open(id: String) {
            if (requested != null) return
            requested = id
            viewModelScope.launch {
                val edit = edits.load(id)
                state.value = if (edit == null) ReportEditLoad.Missing else ReportEditLoad.Ready(edit.toReportUiState())
            }
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

internal fun SubmissionFloor.toReportFloor(): ReportFloor? =
    floorId?.let { ReportFloor.Planned(it) }
        ?: number?.let { ReportFloor.Numbered(number = it, isMezzanine = isMezzanine == true) }

/** Model widoku wczytujący zgłoszenie przyjęte do zmiany w C2 raz na ekran. Wymagania: BR-3, BR-18 */
@HiltViewModel
class ReportReplaceViewModel
    @Inject
    constructor(private val barriers: AcceptedBarriers) : ViewModel() {
        private val state = MutableStateFlow<ReportEditLoad>(ReportEditLoad.Loading)
        private var requested: String? = null

        /** Wynik wczytania zgłoszenia przyjętego. */
        val load: StateFlow<ReportEditLoad> = state.asStateFlow()

        /** Wczytuje zgłoszenie przyjęte [id]; kolejne wywołania nic nie zmieniają. */
        fun open(id: String) {
            if (requested != null) return
            requested = id
            viewModelScope.launch {
                val found = barriers.barriers().firstOrNull { it.id == id }
                state.value = found?.let { ReportEditLoad.Ready(it.toReplaceUiState()) } ?: ReportEditLoad.Missing
            }
        }
    }

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
