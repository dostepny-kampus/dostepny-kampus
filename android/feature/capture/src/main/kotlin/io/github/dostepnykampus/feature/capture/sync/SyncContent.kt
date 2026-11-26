/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.data.submission.sync.SyncStatus
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.campusMoment
import io.github.dostepnykampus.core.designsystem.card.InfoPanel
import io.github.dostepnykampus.feature.capture.R

/**
 * Czynności C4 bez konfliktu.
 *
 * @property onBack „Wstecz”
 * @property onSyncNow „Synchronizuj teraz”
 * @property onLogin „Zaloguj się” przy wygasłym zalogowaniu: L1
 */
data class SyncActions(val onBack: () -> Unit, val onSyncNow: () -> Unit, val onLogin: () -> Unit)

/**
 * Widok C4 bez konfliktu bez modelu widoku: co czeka na wysłanie, ostatnia synchronizacja, stan wysyłki,
 * ostrzeżenie o miejscu i jedna wypełniona czynność na końcu treści. Poza [SyncUiState.Ready] pokazuje sam
 * nagłówek.
 *
 * Wymagania: US9.1, US9.2, US9.4, US9.5, NFR-7, NFR-10, NFR-50, NFR-20
 *
 * @param state stan ekranu
 * @param actions czynności ekranu
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun SyncContent(state: SyncUiState, actions: SyncActions, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.synchronizacja_naglowek), onBack = actions.onBack)
            if (state is SyncUiState.Ready) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m),
                    verticalArrangement = Arrangement.spacedBy(Odstepy.m),
                ) {
                    if (state.phase != SyncPhase.Done) Queue(state.status)
                    PhaseMessage(phase = state.phase, status = state.status)
                    if (state.isLowSpace) {
                        InfoPanel { Text(stringResource(R.string.synchronizacja_miejsce)) }
                    }
                    MainAction(phase = state.phase, actions = actions)
                }
            }
        }
    }
}

@Composable
private fun Queue(status: SyncStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(
            text = stringResource(R.string.moje_czeka, status.waiting),
            fontWeight = Typografia.wyroznienieWaga,
            style = MaterialTheme.typography.titleLarge,
        )
        splitText(status)?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
        status.lastSyncedAt?.let { synced ->
            Text(
                text = stringResource(R.string.synchronizacja_ostatnia, campusMoment(synced)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun splitText(status: SyncStatus): String? {
    val submissions = status.submissions
    val markers = status.markers
    val reports = pluralStringResource(R.plurals.synchronizacja_zgloszenia, submissions, submissions)
    val floors = pluralStringResource(R.plurals.synchronizacja_znaczniki, markers, markers)
    return when {
        submissions > 0 && markers > 0 -> stringResource(R.string.synchronizacja_podzial, reports, floors)
        submissions > 0 -> reports
        markers > 0 -> floors
        else -> null
    }
}

@Composable
private fun PhaseMessage(phase: SyncPhase, status: SyncStatus) {
    val text =
        when (phase) {
            SyncPhase.Waiting -> null
            SyncPhase.Done -> doneText(status)
            is SyncPhase.Sending -> {
                val progress = phase.progress
                stringResource(R.string.synchronizacja_wysylamy, progress.sent, progress.total)
            }
            SyncPhase.Offline -> stringResource(R.string.synchronizacja_bez_internetu)
            SyncPhase.ServerDown -> stringResource(R.string.synchronizacja_serwer)
            SyncPhase.LoginExpired -> stringResource(R.string.synchronizacja_logowanie)
        } ?: return
    InfoPanel(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun doneText(status: SyncStatus): String =
    status.lastSyncedAt?.let { stringResource(R.string.synchronizacja_wszystko_ostatnia, campusMoment(it)) }
        ?: stringResource(R.string.synchronizacja_wszystko)

@Composable
private fun MainAction(phase: SyncPhase, actions: SyncActions) {
    val isLogin = phase == SyncPhase.LoginExpired
    PinnedButton(
        text = stringResource(if (isLogin) R.string.synchronizacja_zaloguj else R.string.synchronizacja_teraz),
        onClick = if (isLogin) actions.onLogin else actions.onSyncNow,
        modifier = Modifier.fillMaxWidth(),
    )
}
