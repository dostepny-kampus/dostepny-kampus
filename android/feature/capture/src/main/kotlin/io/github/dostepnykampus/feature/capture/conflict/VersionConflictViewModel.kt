/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.conflict

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.submission.conflict.VersionConflict
import io.github.dostepnykampus.core.data.submission.conflict.VersionConflicts
import io.github.dostepnykampus.core.designsystem.submission.nameRes
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Stan ekranu C4 przy konflikcie wersji. Wymagania: US9.3, BR-10 */
sealed interface ConflictUiState {
    /** Odczyt z telefonu trwa. */
    data object Loading : ConflictUiState

    /**
     * Obie wersje i nazwy pól, którymi się różnią.
     *
     * @property conflict wersja własna i wersja z serwera
     * @property differences nazwy pól różnicy w kolejności pól zgłoszenia
     * @property isAccepted wersja z serwera jest przyjęta, więc wersja własna trafi do sprawdzenia jako nowa
     */
    @Immutable
    data class Ready(val conflict: VersionConflict, val differences: List<Int>, val isAccepted: Boolean) :
        ConflictUiState

    /** Wybór wykonany albo odłożony; ekran wraca do C3. */
    data object Closed : ConflictUiState

    /** Zgłoszenie nie czeka już na wybór wersji; ekran wraca do C3. */
    data object Missing : ConflictUiState
}

/**
 * Model widoku C4 dla jednego zgłoszenia: wczytuje obie wersje i wykonuje jeden wybór.
 *
 * Wymagania: US9.3, BR-10, NFR-54
 */
@HiltViewModel
class VersionConflictViewModel
    @Inject
    constructor(private val conflicts: VersionConflicts) : ViewModel() {
        private val current = MutableStateFlow<ConflictUiState>(ConflictUiState.Loading)
        private var requested: String? = null
        private var isChosen = false

        /** Stan ekranu. */
        val state: StateFlow<ConflictUiState> = current.asStateFlow()

        /** Wczytuje konflikt zgłoszenia [id]; kolejne wywołania nic nie zmieniają. */
        fun open(id: String) {
            if (requested != null) return
            requested = id
            viewModelScope.launch {
                current.value = conflicts.find(id)?.toUiState() ?: ConflictUiState.Missing
            }
        }

        /** „Zostaw moją wersję”: wersja własna wraca do wysyłki. */
        fun keepMine() {
            choose { conflicts.keepMine(it) }
        }

        /** „Weź wersję z serwera”: zmiana z telefonu znika. */
        fun takeServer() {
            choose { conflicts.takeServer(it) }
        }

        /** „Zdecyduję później”: obie wersje czekają w telefonie. */
        fun later() {
            choose {}
        }

        private fun choose(action: suspend (String) -> Unit) {
            val ready = current.value as? ConflictUiState.Ready ?: return
            if (isChosen) return
            isChosen = true
            viewModelScope.launch {
                action(ready.conflict.id)
                current.value = ConflictUiState.Closed
            }
        }
    }

internal fun VersionConflict.toUiState(): ConflictUiState.Ready =
    ConflictUiState.Ready(
        conflict = this,
        differences = differences.map { it.nameRes() },
        isAccepted = server.status == SubmissionStatus.ACCEPTED,
    )
