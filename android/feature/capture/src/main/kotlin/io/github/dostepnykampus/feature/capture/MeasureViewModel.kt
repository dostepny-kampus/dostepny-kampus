/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.geo.BuildingHint
import io.github.dostepnykampus.core.location.ElapsedClock
import io.github.dostepnykampus.core.location.GnssSystem
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionAverage
import io.github.dostepnykampus.core.location.PositionAveraging
import io.github.dostepnykampus.core.location.PositionFix
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

private const val TICK_MS = 1_000L

/** Stan pomiaru na ekranie C1. Wymagania: US6.1 */
sealed interface MeasureUiState {
    /** „Szukamy satelitów”: nie ma jeszcze odczytu ze znaną dokładnością. */
    data object Searching : MeasureUiState

    /** Lokalizacja w telefonie jest wyłączona; po włączeniu pomiar rusza sam. Wymagania: UC7, NFR-27 */
    data object LocationOff : MeasureUiState

    /**
     * Odbiornik zgłosił błąd, np. brak zgody albo brak GPS w telefonie; „Powtórz pomiar” zaczyna od nowa.
     *
     * Wymagania: NFR-50
     */
    data object Failed : MeasureUiState

    /**
     * Pomiar trwa: „Pomiar: zostało [remainingSeconds] s”.
     *
     * Wymagania: NFR-2, US6.5
     *
     * @property remainingSeconds pełne sekundy do końca pomiaru
     * @property average bieżąca średnia z szacowaną dokładnością
     * @property satellites satelity użyte do pozycji według systemów
     * @property sky wszystkie widoczne satelity do mapy nieba
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
     * Wymagania: US6.5, US6.2
     *
     * @property average średnia z 30 s
     * @property satellites satelity użyte do pozycji według systemów na koniec pomiaru
     * @property sky widoczne satelity na koniec pomiaru
     * @property place budynek zgłoszenia: podpowiedź albo decyzja Zbierającego
     * @property corrected punkt przesunięty przez Zbierającego po pomiarze albo `null` bez korekty (US6.4, NFR-3)
     */
    data class Finished(
        val average: PositionAverage,
        val satellites: Map<GnssSystem, Int>,
        val sky: List<Satellite> = emptyList(),
        val place: PlaceChoice = PlaceChoice.Locating,
        val corrected: GeoPoint? = null,
    ) : MeasureUiState {
        /** Punkt bariery: po korekcie przesunięty, bez niej średnia z pomiaru. Wymagania: US6.4 */
        val point: GeoPoint get() = corrected ?: average.point
    }
}

/** Budynek zgłoszenia po pomiarze. Wymagania: US6.2, BR-G2, BR-G4 */
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

    /**
     * „Poza budynkiem”: wybór Zbierającego albo brak budynku w promieniu 50 m; zgłoszenie bez piętra.
     *
     * Wymagania: BR-G4
     */
    data object Outside : PlaceChoice
}

/**
 * Pomiar pozycji na ekranie C1: 30 s od pierwszego odczytu ze znaną dokładnością, odliczanie co sekundę. Odbiornik
 * nasłuchuje, gdy ekran jest widoczny i 5 s po zniknięciu, a po pomiarze przestaje; wynik zostaje w ViewModelu, a
 * [repeat] zaczyna od nowa. Po pomiarze podpowiada budynek z katalogu bez sieci do potwierdzenia albo zmiany. Błąd
 * odbiornika daje [MeasureUiState.Failed], wyłączona lokalizacja [MeasureUiState.LocationOff] bez nasłuchu, a jej
 * włączenie i przerwany pomiar ([restarted]) zaczynają pomiar od nowa.
 *
 * Wymagania: US6.1, NFR-1, NFR-27, NFR-23, US6.2, NFR-2, NFR-50, UC7
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MeasureViewModel
    @Inject
    constructor(
        private val positions: PositionSource,
        private val satellites: SatelliteSource,
        private val clock: ElapsedClock,
        private val locationSwitch: LocationSwitch,
        private val buildingLookup: BuildingLookup,
    ) : ViewModel() {
        private val result = MutableStateFlow(Run(attempt = 0, finished = null))
        private val catalogBuildings = MutableStateFlow(emptyList<Building>())
        private val nearby = MutableStateFlow(emptyList<BuildingHint>())

        /**
         * Budynki najbliżej punktu do sekcji „Wybierz budynek”, od najbliższego, z odległością; puste, dopóki pomiar
         * się nie skończy. Podpowiedź budynku bierze katalog z pliku albo już pobrany, bez czekania
         * na serwer.
         *
         * Wymagania: US6.2, NFR-4
         */
        val buildings: StateFlow<List<BuildingHint>> = nearby

        private val interrupted = MutableStateFlow(false)

        /**
         * `true`, gdy trwający pomiar przerwano, np. ekran był niewidoczny dłużej niż 5 s albo lokalizację wyłączono,
         * i pomiar zaczął się od nowa; znika po „Powtórz pomiar” i po końcu pomiaru.
         *
         * Wymagania: NFR-23, NFR-27
         */
        val restarted: StateFlow<Boolean> = interrupted

        /** Bieżący stan pomiaru. */
        val uiState: StateFlow<MeasureUiState> =
            result
                .flatMapLatest { run -> run.finished?.let(::flowOf) ?: whenLocationOn(run.attempt) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MeasureUiState.Searching)

        /** „Powtórz pomiar”: nowe szukanie satelitów i pełne 30 s. */
        fun repeat() {
            interrupted.value = false
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

        /** „Poza budynkiem”: zgłoszenie bez budynku i piętra. Wymagania: BR-G2 */
        fun outside() {
            choose { PlaceChoice.Outside }
        }

        /**
         * Korekta ręczna: punkt bariery przesunięty na mapie po pomiarze. Przed końcem pomiaru nic nie zmienia,
         * a „Powtórz pomiar” ją kasuje.
         *
         * Wymagania: US6.4, NFR-3, BR-G2
         *
         * @param point nowe położenie punktu bariery
         */
        fun movePoint(point: GeoPoint) {
            result.update { run -> run.copy(finished = run.finished?.copy(corrected = point)) }
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
                val (buildings, hints) = buildingLookup.measure(point)
                if (result.value.attempt == attempt) {
                    catalogBuildings.value = buildings
                    nearby.value = hints.nearby
                    val place = hints.suggested?.let(PlaceChoice::Suggested) ?: PlaceChoice.Outside
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
        }

        private fun whenLocationOn(attempt: Int): Flow<MeasureUiState> =
            locationSwitch
                .isEnabled()
                .distinctUntilChanged()
                .flatMapLatest { isOn ->
                    if (isOn) {
                        measurement(attempt).onCompletion { cause ->
                            val run = result.value
                            if (cause != null && run.attempt == attempt && run.finished == null) {
                                interrupted.value = true
                            }
                        }
                    } else {
                        flowOf(MeasureUiState.LocationOff)
                    }
                }

        private fun measurement(attempt: Int): Flow<MeasureUiState> =
            channelFlow {
                var averaging: PositionAveraging? = null
                var sky = emptyList<Satellite>()

                suspend fun publish() = send(stateOf(averaging, sky, clock.nowMillis()))

                publish()
                val others =
                    launch {
                        launch {
                            satellites.satellites().collect { visible ->
                                sky = visible
                                publish()
                            }
                        }
                        while (true) {
                            delay(TICK_MS)
                            publish()
                        }
                    }
                val isCollected =
                    collectFixes { fix ->
                        val current = averaging ?: PositionAveraging(fix.elapsedMillis).takeIf { fix.hasAccuracy }
                        averaging = current?.withFix(fix)
                        publish()
                    }
                if (!isCollected) {
                    others.cancel()
                    send(MeasureUiState.Failed)
                }
            }.takeWhile { state ->
                if (state is MeasureUiState.Finished) finish(attempt, state)
                state !is MeasureUiState.Finished
            }

        private suspend fun collectFixes(onFix: suspend (PositionFix) -> Unit): Boolean =
            try {
                positions.fixes().collect { onFix(it) }
                true
            } catch (_: SecurityException) {
                false
            } catch (_: IllegalArgumentException) {
                false
            }

        private fun finish(attempt: Int, state: MeasureUiState.Finished) {
            val run = result.value
            if (run.attempt != attempt || run.finished != null) return
            if (result.compareAndSet(expect = run, update = run.copy(finished = state))) {
                interrupted.value = false
                locate(attempt, state.average.point)
            }
        }

        private data class Run(val attempt: Int, val finished: MeasureUiState.Finished?)
    }

/** Stan C1 z uśredniania [averaging] i nieba [sky] w chwili [now]. Wymagania: US6.1, US6.5 */
internal fun stateOf(averaging: PositionAveraging?, sky: List<Satellite>, now: Long): MeasureUiState {
    val average = averaging?.average() ?: return MeasureUiState.Searching
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
