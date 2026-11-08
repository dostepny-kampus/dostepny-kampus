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
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

/**
 * Krok 3 V4: bieżąca pozycja i „Wyślij” (US4.1, NFR-27). Pozycja to pierwszy odczyt ze znaną dokładnością, a budynek
 * z konturu w katalogu bez sieci decyduje, czy piętro idzie do zgłoszenia (BR-14, BR-G2). Odbiornik nasłuchuje, gdy
 * ekran jest widoczny; „Wyślij” czeka na pozycję bez limitu czasu (NFR-23).
 */
@HiltViewModel
class QuickSendViewModel
    @Inject
    constructor(
        positions: PositionSource,
        private val catalog: BuildingCatalogRepository,
        private val saver: ReportSaver,
    ) : ViewModel() {
        private val state = MutableStateFlow(QuickSendStage.READY)

        /** Etap zapisu. */
        val stage: StateFlow<QuickSendStage> = state.asStateFlow()

        /** Bieżąca pozycja albo `null`, dopóki telefon jej nie poda. */
        val position: StateFlow<QuickPosition?> =
            positions
                .fixes()
                .filter(PositionFix::hasAccuracy)
                .map { fix -> located(fix) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

        /** „Wyślij”: zapis w kolejce z bieżącą pozycją; bez pozycji i w czasie zapisu nic nie robi. */
        fun send(report: QuickReport) {
            val current = position.value ?: return
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

        private suspend fun located(fix: PositionFix): QuickPosition {
            val buildings = catalog.catalogUpdates().first().buildings
            val isInside = hintBuilding(fix.point, buildings)?.isInside == true
            return QuickPosition(point = fix.point, accuracyMeters = fix.accuracyMeters, isInBuilding = isInside)
        }
    }
