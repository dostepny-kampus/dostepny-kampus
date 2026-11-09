/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.geo.BuildingHint
import io.github.dostepnykampus.core.geo.hintBuilding
import io.github.dostepnykampus.core.location.ElapsedClock
import io.github.dostepnykampus.core.location.GnssSystem
import io.github.dostepnykampus.core.location.PositionAverage
import io.github.dostepnykampus.core.location.PositionAveraging
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.location.Satellite
import io.github.dostepnykampus.core.location.SatelliteSource
import io.github.dostepnykampus.core.location.countsInFix
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
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
     * @property sky wszystkie widoczne satelity do mapy nieba (US6.5)
     */
    data class Measuring(
        val remainingSeconds: Int,
        val average: PositionAverage,
        val satellites: Map<GnssSystem, Int>,
        val sky: List<Satellite> = emptyList(),
    ) : MeasureUiState

    /**
     * „Pomiar zakończony”: wynik i satelity z końca pomiaru.
     *
     * @property average średnia z 30 s
     * @property satellites satelity użyte do pozycji według systemów na koniec pomiaru
     * @property sky widoczne satelity na koniec pomiaru (US6.5)
     * @property place budynek zgłoszenia: podpowiedź albo decyzja Zbierającego (US6.2)
     */
    data class Finished(
        val average: PositionAverage,
        val satellites: Map<GnssSystem, Int>,
        val sky: List<Satellite> = emptyList(),
        val place: PlaceChoice = PlaceChoice.Locating,
    ) : MeasureUiState
}

/** Budynek zgłoszenia po pomiarze (US6.2, BR-G2, BR-G4). */
sealed interface PlaceChoice {
    /** Podpowiedź jeszcze się liczy. */
    data object Locating : PlaceChoice

    /**
     * „Czy to budynek …?”: podpowiedź czeka na decyzję.
     *
     * @property hint budynek z odległością od punktu
     */
    data class Suggested(val hint: BuildingHint) : PlaceChoice

    /**
     * Budynek potwierdzony albo wybrany z listy.
     *
     * @property building budynek zgłoszenia
     */
    data class Chosen(val building: Building) : PlaceChoice

    /** „Poza budynkiem”: wybór Zbierającego albo brak budynku w promieniu 50 m; zgłoszenie bez piętra (BR-G4). */
    data object Outside : PlaceChoice
}

/**
 * Pomiar pozycji na ekranie C1 (US6.1, NFR-1). 30 s liczy się od pierwszego odczytu ze znaną dokładnością, więc czas
 * szukania satelitów nie skraca pomiaru. Odbiornik nasłuchuje, gdy ekran jest widoczny i jeszcze 5 s po zniknięciu
 * (obrót ekranu), a po końcu pomiaru przestaje (NFR-27). Wynik zostaje w ViewModelu, więc powrót na ekran go nie
 * kasuje; [repeat] zaczyna od nowa bez limitu (NFR-23). Po pomiarze podpowiada budynek z katalogu bez sieci,
 * a Zbierający go potwierdza, wybiera inny albo zostawia „poza budynkiem” (US6.2).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MeasureViewModel
    @Inject
    constructor(
        private val positions: PositionSource,
        private val satellites: SatelliteSource,
        private val clock: ElapsedClock,
        private val catalog: BuildingCatalogRepository,
        private val draft: ReportDraft = ReportDraft(),
    ) : ViewModel() {
        private val result = MutableStateFlow(Run(attempt = 0, finished = null))
        private val catalogBuildings = MutableStateFlow(emptyList<Building>())

        /**
         * Budynki z katalogu do „Wybierz inny”; puste, dopóki pomiar się nie skończy (US6.2). Podpowiedź budynku bierze
         * katalog z pliku albo już pobrany, bez czekania na serwer (NFR-4: do 2 s bez sieci).
         */
        val buildings: StateFlow<List<Building>> = catalogBuildings

        /** Bieżący stan pomiaru. */
        val uiState: StateFlow<MeasureUiState> =
            result
                .flatMapLatest { run -> run.finished?.let(::flowOf) ?: measurement(run.attempt) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MeasureUiState.Searching)

        /** „Powtórz pomiar”: nowe szukanie satelitów i pełne 30 s. */
        fun repeat() {
            result.update { Run(attempt = it.attempt + 1, finished = null) }
        }

        /** „Potwierdź”: podpowiedziany budynek staje się budynkiem zgłoszenia. */
        fun confirm() {
            choose { place -> (place as? PlaceChoice.Suggested)?.let { PlaceChoice.Chosen(it.hint.building) } }
        }

        /**
         * „Wybierz inny”: budynek z listy zamiast podpowiedzi.
         *
         * @param symbol symbol wybranego budynku
         */
        fun choose(symbol: String) {
            val building = catalogBuildings.value.firstOrNull { it.symbol == symbol } ?: return
            choose { PlaceChoice.Chosen(building) }
        }

        /** „Poza budynkiem”: zgłoszenie bez budynku i piętra (BR-G2). */
        fun outside() {
            choose { PlaceChoice.Outside }
        }

        /** Wyjście z C1 kończy zgłoszenie, więc pola odłożone przez „Zmień miejsce” na C2 już nie wrócą (US7.4). */
        override fun onCleared() {
            draft.clear()
        }

        private fun choose(change: (PlaceChoice) -> PlaceChoice?) {
            result.update { run ->
                val finished = run.finished
                val place = finished?.let { change(it.place) }
                if (finished != null && place != null) run.copy(finished = finished.copy(place = place)) else run
            }
        }

        private fun locate(attempt: Int, point: GeoPoint) {
            viewModelScope.launch {
                val buildings = catalog.catalogUpdates().first().buildings
                catalogBuildings.value = buildings
                val place = hintBuilding(point, buildings)?.let(PlaceChoice::Suggested) ?: PlaceChoice.Outside
                result.update { run ->
                    val finished = run.finished
                    if (run.attempt == attempt && finished?.place == PlaceChoice.Locating) {
                        run.copy(finished = finished.copy(place = place))
                    } else {
                        run
                    }
                }
            }
        }

        private fun measurement(attempt: Int): Flow<MeasureUiState> =
            channelFlow {
                var averaging: PositionAveraging? = null
                var sky = emptyList<Satellite>()

                suspend fun publish() = send(stateOf(averaging, sky))

                publish()
                launch {
                    satellites.satellites().collect { visible ->
                        sky = visible
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
                    locate(attempt, state.average.point)
                }
            }

        private fun stateOf(averaging: PositionAveraging?, sky: List<Satellite>): MeasureUiState {
            val average = averaging?.average() ?: return MeasureUiState.Searching
            val now = clock.nowMillis()
            return if (averaging.isFinished(now)) {
                MeasureUiState.Finished(average, sky.countsInFix(), sky)
            } else {
                MeasureUiState.Measuring(
                    remainingSeconds = averaging.remainingSeconds(now),
                    average = average,
                    satellites = sky.countsInFix(),
                    sky = sky,
                )
            }
        }

        private data class Run(val attempt: Int, val finished: MeasureUiState.Finished?)
    }
