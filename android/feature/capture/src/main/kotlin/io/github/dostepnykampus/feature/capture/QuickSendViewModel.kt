/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.buildings.BuildingFloors
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.model.Floor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/** Etap kroku 3 V4. Wymagania: US4.1, US4.4 */
enum class QuickSendStage {
    /** Podsumowanie przed „Wyślij”. */
    READY,

    /** Zapis w kolejce trwa; „Wyślij” jest nieczynne, żeby nie wysłać zdjęcia drugi raz. Wymagania: US4.7 */
    SENDING,

    /** Zapisane w kolejce przy internecie; dalej „Dziękujemy…”. Wymagania: US4.4 */
    SENT,

    /**
     * Zapisane w kolejce bez internetu; dalej „Zapisaliśmy zdjęcie w telefonie…”, a wysyła kolejka.
     *
     * Wymagania: US4.2
     */
    SAVED_OFFLINE,

    /** „Za mało miejsca w telefonie” z ponowieniem. Wymagania: NFR-7 */
    FAILED,
}

/** Ustalanie pozycji w kroku 3 V4. Wymagania: NFR-23 */
sealed interface QuickLookup {
    /** „Ustalamy, gdzie jesteś…”; „Wyślij” czeka. */
    data object Locating : QuickLookup

    /** Telefon zgłosił, że nie ustali pozycji: błąd odbiornika albo wyłączona lokalizacja. */
    data object Lost : QuickLookup

    /**
     * Pozycja do zgłoszenia.
     *
     * @property position bieżąca pozycja
     */
    data class Found(val position: QuickPosition) : QuickLookup
}

/**
 * Pozycja w krokach 1–2 V4 i krok 3: bieżąca pozycja i „Wyślij”. Krok 2 pomija pytanie o piętro,
 * gdy pozycja jest już znana i leży poza konturem, a w budynku szczegółowym pokazuje jego piętra z serwera.
 * Pozycja to pierwszy odczyt ze znaną dokładnością, a budynek z konturu w katalogu bez sieci decyduje, czy piętro
 * idzie do zgłoszenia. Odbiornik nasłuchuje, gdy ekran jest widoczny; „Wyślij” czeka na pozycję bez
 * limitu czasu. „Brak pozycji” wywołuje zdarzenie odbiornika albo wyłączenie lokalizacji, nie upływ czasu;
 * „Spróbuj ponownie” ustala pozycję od nowa.
 *
 * Wymagania: US4.1, NFR-27, US4.5, BR-14, BR-G2, NFR-23
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuickSendViewModel
    @Inject
    constructor(
        private val positions: PositionSource,
        private val locationSwitch: LocationSwitch,
        private val saver: ReportSaver,
        private val networkStatus: NetworkStatus,
        private val buildingFloors: BuildingFloors,
        private val buildingLookup: BuildingLookup,
    ) : ViewModel() {
        private val state = MutableStateFlow(QuickSendStage.READY)

        /** Etap zapisu. */
        val stage: StateFlow<QuickSendStage> = state.asStateFlow()

        private val attempts = MutableStateFlow(0)

        private val lookups: Flow<QuickLookup> = attempts.flatMapLatest { whenLocationOn() }

        /** Bieżąca pozycja, jej ustalanie albo „Brak pozycji”. */
        val lookup: StateFlow<QuickLookup> =
            lookups.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), QuickLookup.Locating)

        /** Piętra budynku z pozycji od najniższego; pusta lista to budynek podstawowy albo pozycja nieznana. */
        val floorChoices: StateFlow<List<Floor>> =
            lookup
                .map { (it as? QuickLookup.Found)?.run { position.buildingSymbol } }
                .distinctUntilChanged()
                .mapLatest { symbol -> floorsOf(symbol) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        /** „Spróbuj ponownie” po „Brak pozycji”: ustalanie od nowa; zdjęcie i grupa zostają. */
        fun retry() {
            attempts.value++
        }

        /** „Wyślij”: zapis w kolejce z bieżącą pozycją; bez pozycji i w czasie zapisu nic nie robi. */
        fun send(report: QuickReport) {
            val current = (lookup.value as? QuickLookup.Found)?.position ?: return
            if (state.value != QuickSendStage.READY && state.value != QuickSendStage.FAILED) return
            state.value = QuickSendStage.SENDING
            viewModelScope.launch {
                state.value =
                    try {
                        saver.save(report.content(current), listOf(report.photo), null)
                        if (networkStatus.isOnline().first()) QuickSendStage.SENT else QuickSendStage.SAVED_OFFLINE
                    } catch (_: IOException) {
                        QuickSendStage.FAILED
                    } catch (_: SQLiteException) {
                        QuickSendStage.FAILED
                    }
            }
        }

        private fun whenLocationOn(): Flow<QuickLookup> =
            locationSwitch
                .isEnabled()
                .distinctUntilChanged()
                .flatMapLatest { isOn -> if (isOn) locating() else flowOf(QuickLookup.Lost) }

        private fun locating(): Flow<QuickLookup> =
            positions
                .fixes()
                .filter { it.hasAccuracy }
                .take(1)
                .map<PositionFix, QuickLookup> { fix -> QuickLookup.Found(located(fix)) }
                .onStart { emit(QuickLookup.Locating) }
                .catch { cause ->
                    if (cause !is SecurityException && cause !is IllegalArgumentException) throw cause
                    emit(QuickLookup.Lost)
                }

        private suspend fun floorsOf(symbol: String?): List<Floor> =
            if (symbol == null) emptyList() else buildingFloors.floors(symbol)?.floors.orEmpty()

        private suspend fun located(fix: PositionFix): QuickPosition {
            val inside = buildingLookup.containing(fix.point)
            return QuickPosition(
                point = fix.point,
                accuracyMeters = fix.accuracyMeters,
                isInBuilding = inside != null,
                buildingSymbol = inside?.symbol,
            )
        }
    }
