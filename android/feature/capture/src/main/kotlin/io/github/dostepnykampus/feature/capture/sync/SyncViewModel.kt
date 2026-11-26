/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.data.photo.PhotoSpace
import io.github.dostepnykampus.core.data.submission.sync.SyncNow
import io.github.dostepnykampus.core.data.submission.sync.SyncProgress
import io.github.dostepnykampus.core.data.submission.sync.SyncStatus
import io.github.dostepnykampus.core.data.submission.sync.SyncStatusSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/** Czy wysyłka czeka na ponowne zalogowanie. Wymagania: US9.4, BR-26 */
fun interface LoginWait {
    /** Prawda od odmowy 401 do ponownego zalogowania. */
    fun isExpired(): Flow<Boolean>
}

/** Co dzieje się z kolejką wysyłki na C4 bez konfliktu. */
sealed interface SyncPhase {
    /** Zwykły stan: coś czeka, wysyłka ruszy sama. */
    data object Waiting : SyncPhase

    /** „Wszystko wysłane”. */
    data object Done : SyncPhase

    /**
     * „Wysyłamy X z Y…”.
     *
     * @property progress postęp przebiegu
     */
    data class Sending(val progress: SyncProgress) : SyncPhase

    /** „Bez internetu. Wyślemy wszystko sami, gdy internet wróci.” */
    data object Offline : SyncPhase

    /** „Serwer chwilowo nie odpowiada…”. */
    data object ServerDown : SyncPhase

    /** „Logowanie wygasło…” z „Zaloguj się”. */
    data object LoginExpired : SyncPhase
}

/** Stan ekranu C4 bez konfliktu. */
sealed interface SyncUiState {
    /** Pierwszy odczyt kolejki trwa. */
    data object Loading : SyncUiState

    /**
     * Kolejka wczytana.
     *
     * @property status liczba czekających i ostatnia synchronizacja
     * @property phase co dzieje się z wysyłką
     * @property isLowSpace ostrzeżenie o mniej niż 100 MB wolnego miejsca
     */
    data class Ready(val status: SyncStatus, val phase: SyncPhase, val isLowSpace: Boolean) : SyncUiState
}

/**
 * C4 bez konfliktu: co czeka na wysłanie, postęp, problemy z wysyłką i „Synchronizuj teraz”. Dane tylko z
 * `core:data`; wysyłka rusza też sama.
 *
 * Wymagania: US9.1, US9.2, US9.4, US9.5, NFR-7, NFR-10, NFR-50
 */
@HiltViewModel
class SyncViewModel
    @Inject
    constructor(
        status: SyncStatusSource,
        private val sync: SyncNow,
        network: NetworkStatus,
        login: LoginWait,
        space: PhotoSpace,
    ) : ViewModel() {
        /** Stan ekranu zmieniający się po każdym zapisie, wysyłce i zmianie sieci. */
        val state: StateFlow<SyncUiState> =
            combine(
                flow = status.status(),
                flow2 = sync.progress(),
                flow3 = network.isOnline(),
                flow4 = login.isExpired(),
            ) { now, run, net, out ->
                val phase = syncPhaseOf(status = now, progress = run, isOnline = net, isLoginExpired = out)
                SyncUiState.Ready(status = now, phase = phase, isLowSpace = space.isLow())
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SyncUiState.Loading)

        /** „Synchronizuj teraz”; gdy wysyłka trwa, nic nie zmienia. Wymagania: US9.2 */
        fun syncNow() {
            viewModelScope.launch { sync.syncNow() }
        }
    }

/**
 * Stan wysyłki: wygasłe zalogowanie przed trwającym przebiegiem, pusta kolejka przed brakiem sieci, a brak sieci
 * przed błędem serwera, bo przerwany przebieg bez sieci nie jest winą serwera.
 *
 * Wymagania: US9.1, US9.4, BR-26, NFR-50
 */
internal fun syncPhaseOf(
    status: SyncStatus,
    progress: SyncProgress?,
    isOnline: Boolean,
    isLoginExpired: Boolean,
): SyncPhase =
    when {
        isLoginExpired -> SyncPhase.LoginExpired
        progress != null -> SyncPhase.Sending(progress)
        status.waiting == 0 -> SyncPhase.Done
        !isOnline -> SyncPhase.Offline
        status.isLastRunFailed -> SyncPhase.ServerDown
        else -> SyncPhase.Waiting
    }
