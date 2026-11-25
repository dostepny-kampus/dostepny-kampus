/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.export.BarrierExport
import io.github.dostepnykampus.core.data.export.ExportOutcome
import io.github.dostepnykampus.core.model.export.ExportFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Etap pobrania eksportu na K3. Wymagania: US13, NFR-50 */
enum class ExportStage {
    /** Wybór formatu przed pobraniem. */
    IDLE,

    /** Serwer przygotowuje plik; drugie pobranie nie startuje. */
    LOADING,

    /** Plik zapisany w „Pobrane”. */
    SAVED,

    /** Plik zapisany, ale bez żadnej bariery. */
    EMPTY,

    /** Bez internetu. */
    OFFLINE,

    /** Błąd serwera albo zapisu pliku. */
    SERVER_ERROR,

    /** Konto nie ma roli Koordynatora. */
    FORBIDDEN,
}

/**
 * Stan ekranu K3.
 *
 * @property format wybrany format pliku
 * @property stage etap pobrania
 */
data class ExportUiState(val format: ExportFormat = ExportFormat.GEOJSON, val stage: ExportStage = ExportStage.IDLE)

/**
 * K3 „Eksport”: wybór formatu i pobranie pliku do „Pobrane”.
 *
 * TODO(#3062): serwer nie ma jeszcze eksportu GeoJSON, więc jego pobranie kończy się błędem serwera.
 *
 * Wymagania: US13, US13.1, US13.2, NFR-50
 *
 * @param export pobranie eksportu
 */
@HiltViewModel
class ExportViewModel
    @Inject
    constructor(private val export: BarrierExport) : ViewModel() {
        private val current = MutableStateFlow(ExportUiState())

        /** Stan ekranu. */
        val state: StateFlow<ExportUiState> = current.asStateFlow()

        /** Wybiera format [format] następnego pobrania. */
        fun choose(format: ExportFormat) {
            current.update { it.copy(format = format) }
        }

        /** „Pobierz plik” i „Spróbuj ponownie”; w trakcie pobrania kolejne dotknięcie nic nie robi. */
        fun download() {
            if (current.value.stage == ExportStage.LOADING) return
            val format = current.value.format
            current.update { it.copy(stage = ExportStage.LOADING) }
            viewModelScope.launch {
                val stage = stageOf(export.download(format))
                current.update { it.copy(stage = stage) }
            }
        }
    }

internal fun stageOf(outcome: ExportOutcome): ExportStage =
    when (outcome) {
        is ExportOutcome.Saved -> if (outcome.isEmpty) ExportStage.EMPTY else ExportStage.SAVED
        ExportOutcome.Offline -> ExportStage.OFFLINE
        ExportOutcome.Forbidden -> ExportStage.FORBIDDEN
        ExportOutcome.Failed -> ExportStage.SERVER_ERROR
    }
