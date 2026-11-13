/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.submissions.PendingEdit
import io.github.dostepnykampus.core.data.submissions.PendingEdits
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.core.model.SubmissionFloor
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

private fun SubmissionFloor.toReportFloor(): ReportFloor? =
    floorId?.let { ReportFloor.Planned(it) }
        ?: number?.let { ReportFloor.Numbered(number = it, isMezzanine = isMezzanine == true) }
