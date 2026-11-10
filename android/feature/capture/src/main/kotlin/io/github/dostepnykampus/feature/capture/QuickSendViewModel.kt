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
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.geo.hintBuilding
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/** Etap kroku 3 V4 (US4.1, US4.4). */
enum class QuickSendStage {
    /** Podsumowanie przed „Wyślij”. */
    READY,

    /** Zapis w kolejce trwa; „Wyślij” jest nieczynne, żeby nie wysłać zdjęcia drugi raz (US4.7). */
    SENDING,

    /** Zapisane w kolejce; dalej potwierdzenie, a wysyłkę po powrocie sieci robi kolejka (US4.2). */
    SENT,

    /** „Za mało miejsca w telefonie” z ponowieniem (NFR-7). */
    FAILED,
}

/** Ustalanie pozycji w kroku 3 V4 (NFR-23). */
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
 * Krok 3 V4: bieżąca pozycja i „Wyślij” (US4.1, NFR-27). Pozycja to pierwszy odczyt ze znaną dokładnością, a budynek
 * z konturu w katalogu bez sieci decyduje, czy piętro idzie do zgłoszenia (BR-14, BR-G2). Odbiornik nasłuchuje, gdy
 * ekran jest widoczny; „Wyślij” czeka na pozycję bez limitu czasu (NFR-23). „Brak pozycji” wywołuje zdarzenie
 * odbiornika albo wyłączenie lokalizacji, nie upływ czasu; „Spróbuj ponownie” ustala pozycję od nowa.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuickSendViewModel
    @Inject
    constructor(
        private val positions: PositionSource,
        locationSwitch: LocationSwitch,
        private val catalog: BuildingCatalogRepository,
        private val saver: ReportSaver,
    ) : ViewModel() {
        private val state = MutableStateFlow(QuickSendStage.READY)

        /** Etap zapisu. */
        val stage: StateFlow<QuickSendStage> = state.asStateFlow()

        private val attempts = MutableStateFlow(0)

        /** Bieżąca pozycja, jej ustalanie albo „Brak pozycji”. */
        val lookup: StateFlow<QuickLookup> =
            attempts
                .flatMapLatest {
                    locationSwitch
                        .isEnabled()
                        .distinctUntilChanged()
                        .flatMapLatest { isOn -> if (isOn) locating() else flowOf(QuickLookup.Lost) }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), QuickLookup.Locating)

        /** „Spróbuj ponownie” po „Brak pozycji”: ustalanie od nowa; zdjęcie i grupa zostają. */
        fun retry() {
            attempts.value++
        }

        /** „Wyślij”: zapis w kolejce z bieżącą pozycją; bez pozycji i w czasie zapisu nic nie robi. */
        fun send(report: QuickReport) {
            val current = (lookup.value as? QuickLookup.Found)?.position ?: return
            if (state.value == QuickSendStage.SENDING || state.value == QuickSendStage.SENT) return
            state.value = QuickSendStage.SENDING
            viewModelScope.launch {
                state.value =
                    try {
                        saver.save(report.content(current), listOf(report.photo), null)
                        QuickSendStage.SENT
                    } catch (_: IOException) {
                        QuickSendStage.FAILED
                    } catch (_: SQLiteException) {
                        QuickSendStage.FAILED
                    }
            }
        }

        private fun locating(): Flow<QuickLookup> =
            positions
                .fixes()
                .filter { it.hasAccuracy }
                .map<PositionFix, QuickLookup> { fix -> QuickLookup.Found(located(fix)) }
                .onStart { emit(QuickLookup.Locating) }
                .catch { cause ->
                    if (cause !is SecurityException && cause !is IllegalArgumentException) throw cause
                    emit(QuickLookup.Lost)
                }

        private suspend fun located(fix: PositionFix): QuickPosition {
            val buildings = catalog.catalogUpdates().first().buildings
            val isInside = hintBuilding(fix.point, buildings)?.isInside == true
            return QuickPosition(point = fix.point, accuracyMeters = fix.accuracyMeters, isInBuilding = isInside)
        }
    }
