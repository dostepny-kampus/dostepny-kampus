/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.location.ElapsedClock
import io.github.dostepnykampus.core.location.GnssSystem
import io.github.dostepnykampus.core.location.PositionAverage
import io.github.dostepnykampus.core.location.PositionAveraging
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.location.SatelliteSource
import io.github.dostepnykampus.core.location.countsInFix
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

// Odliczanie „Pomiar: zostało 12 s” zmienia się co sekundę także bez nowych odczytów (NFR-2).
private const val TICK_MS = 1_000L

/** Stan pomiaru na ekranie C1 (US6.1). */
sealed interface MeasureUiState {
    /** „Szukamy satelitów”: nie ma jeszcze odczytu ze znaną dokładnością. */
    data object Searching : MeasureUiState

    /**
     * Pomiar trwa: „Pomiar: zostało [remainingSeconds] s”.
     *
     * @property remainingSeconds pełne sekundy do końca pomiaru
     * @property average bieżąca średnia z szacowaną dokładnością (NFR-2)
     * @property satellites satelity użyte do pozycji według systemów
     */
    data class Measuring(
        val remainingSeconds: Int,
        val average: PositionAverage,
        val satellites: Map<GnssSystem, Int>,
    ) : MeasureUiState

    /**
     * „Pomiar zakończony”: wynik i satelity z końca pomiaru.
     *
     * @property average średnia z 30 s
     * @property satellites satelity użyte do pozycji według systemów na koniec pomiaru
     */
    data class Finished(val average: PositionAverage, val satellites: Map<GnssSystem, Int>) : MeasureUiState
}

/**
 * Pomiar pozycji na ekranie C1 (US6.1, NFR-1). 30 s liczy się od pierwszego odczytu ze znaną dokładnością, więc czas
 * szukania satelitów nie skraca pomiaru. Odbiornik nasłuchuje, gdy ekran jest widoczny i jeszcze 5 s po zniknięciu
 * (obrót ekranu), a po końcu pomiaru przestaje (NFR-27). Wynik zostaje w ViewModelu, więc powrót na ekran go nie
 * kasuje; [repeat] zaczyna od nowa bez limitu (NFR-23).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MeasureViewModel
    @Inject
    constructor(
        private val positions: PositionSource,
        private val satellites: SatelliteSource,
        private val clock: ElapsedClock,
    ) : ViewModel() {
        private val result = MutableStateFlow(Run(attempt = 0, finished = null))

        /** Bieżący stan pomiaru. */
        val uiState: StateFlow<MeasureUiState> =
            result
                .flatMapLatest { run -> run.finished?.let(::flowOf) ?: measurement(run.attempt) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MeasureUiState.Searching)

        /** „Powtórz pomiar”: nowe szukanie satelitów i pełne 30 s. */
        fun repeat() {
            result.update { Run(attempt = it.attempt + 1, finished = null) }
        }

        private fun measurement(attempt: Int): Flow<MeasureUiState> =
            channelFlow {
                var averaging: PositionAveraging? = null
                var counts = emptyMap<GnssSystem, Int>()

                suspend fun publish() = send(stateOf(averaging, counts))

                publish()
                launch {
                    satellites.satellites().collect { visible ->
                        counts = visible.countsInFix()
                        publish()
                    }
                }
                launch {
                    positions.fixes().collect { fix ->
                        val current = averaging ?: PositionAveraging(fix.elapsedMillis).takeIf { fix.hasAccuracy }
                        averaging = current?.withFix(fix)
                        publish()
                    }
                }
                while (true) {
                    delay(TICK_MS)
                    publish()
                }
            }.onEach { state ->
                if (state is MeasureUiState.Finished) {
                    result.update { if (it.attempt == attempt) it.copy(finished = state) else it }
                }
            }

        private fun stateOf(averaging: PositionAveraging?, counts: Map<GnssSystem, Int>): MeasureUiState {
            val average = averaging?.average() ?: return MeasureUiState.Searching
            val now = clock.nowMillis()
            return if (averaging.isFinished(now)) {
                MeasureUiState.Finished(average, counts)
            } else {
                MeasureUiState.Measuring(averaging.remainingSeconds(now), average, counts)
            }
        }

        private data class Run(val attempt: Int, val finished: MeasureUiState.Finished?)
    }
