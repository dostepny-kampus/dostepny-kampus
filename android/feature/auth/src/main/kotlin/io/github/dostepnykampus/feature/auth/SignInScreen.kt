/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.auth

import androidx.annotation.StringRes
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.data.account.SignInEntry
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.OfflinePill
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedActionBar
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.bar.PinnedButtonKind
import io.github.dostepnykampus.core.designsystem.card.InfoPanel
import io.github.dostepnykampus.core.designsystem.layout.readableWidth

/**
 * L1 „Prośba o logowanie” ze stanem z modelu widoku.
 *
 * Wymagania: US14.2, US14.3
 *
 * @param isForReport wejście ze „Zgłoś barierę”
 * @param actions czynności prowadzące poza ekran
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel stan logowania; w aplikacji z grafu Hilt
 */
@Composable
fun SignInScreen(
    isForReport: Boolean,
    actions: SignInActions,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SignInContent(
        state = state,
        isForReport = isForReport,
        actions = actions,
        onOpen = viewModel::open,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

/**
 * Widok L1 bez modelu widoku: prośba o konto, brak sieci i błąd logowania.
 *
 * Wymagania: US14.2, US14.3, UC6, UC7, NFR-31, NFR-50, NFR-52
 *
 * @param state stan ekranu
 * @param isForReport wejście ze „Zgłoś barierę”
 * @param actions czynności prowadzące poza ekran
 * @param onOpen „Zaloguj się” i „Załóż konto”
 * @param onRetry „Spróbuj ponownie” po błędzie
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun SignInContent(
    state: SignInUiState,
    isForReport: Boolean,
    actions: SignInActions,
    onOpen: (SignInEntry) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isInContent = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val choices: @Composable (Boolean) -> Unit = { isPinned ->
        Choices(state = state, onLeave = actions.onLeave, onOpen = onOpen, onRetry = onRetry, isPinned = isPinned)
    }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.readableWidth().windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.logowanie_naglowek), onBack = actions.onBack)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m),
                verticalArrangement = Arrangement.spacedBy(Odstepy.m),
            ) {
                Message(state = state, isForReport = isForReport, onLeave = actions.onLeave)
                if (isInContent) choices(false)
            }
            if (!isInContent) choices(true)
        }
    }
}

private data class Choice(@param:StringRes val text: Int, val onClick: () -> Unit, val kind: PinnedButtonKind)

@Composable
private fun Message(state: SignInUiState, isForReport: Boolean, onLeave: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
        when {
            !state.isOnline -> {
                OfflinePill()
                SignInPanel(title = R.string.logowanie_wymaga_internetu, text = R.string.logowanie_mapa_bez_internetu) {
                    PinnedButton(
                        text = stringResource(R.string.logowanie_wroc_do_mapy),
                        onClick = onLeave,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            state.stage == SignInStage.FAILED -> {
                SignInPanel(
                    title = R.string.logowanie_nie_udalo,
                    text = R.string.logowanie_sprawdz_internet,
                    isAlert = true,
                )
            }

            else -> {
                SignInPanel(title = R.string.logowanie_potrzebne_konto, text = R.string.logowanie_konto_chroni)
                if (isForReport) {
                    Text(
                        text = stringResource(R.string.logowanie_trzy_kroki),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun SignInPanel(
    @StringRes title: Int,
    @StringRes text: Int,
    isAlert: Boolean = false,
    content: (@Composable () -> Unit)? = null,
) {
    val live = if (isAlert) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier
    InfoPanel(modifier = live) {
        Text(
            text = stringResource(title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        Text(text = stringResource(text), style = MaterialTheme.typography.bodyLarge)
        content?.invoke()
    }
}

@Composable
private fun Choices(
    state: SignInUiState,
    onLeave: () -> Unit,
    onOpen: (SignInEntry) -> Unit,
    onRetry: () -> Unit,
    isPinned: Boolean,
) {
    val buttons =
        when {
            !state.isOnline -> {
                return
            }

            state.stage == SignInStage.FAILED -> {
                listOf(
                    Choice(R.string.logowanie_sprobuj_ponownie, onRetry, PinnedButtonKind.Filled),
                    Choice(R.string.logowanie_nie_teraz, onLeave, PinnedButtonKind.Outlined),
                )
            }

            else -> {
                listOf(
                    Choice(R.string.logowanie_zaloguj, { onOpen(SignInEntry.SIGN_IN) }, PinnedButtonKind.Filled),
                    Choice(R.string.logowanie_zaloz_konto, { onOpen(SignInEntry.REGISTER) }, PinnedButtonKind.Tonal),
                    Choice(R.string.logowanie_nie_teraz, onLeave, PinnedButtonKind.Outlined),
                )
            }
        }
    val each: @Composable (Modifier) -> Unit = { width ->
        buttons.forEach { choice ->
            PinnedButton(
                text = stringResource(choice.text),
                onClick = choice.onClick,
                modifier = width,
                kind = choice.kind,
            )
        }
    }
    if (isPinned) {
        PinnedActionBar { each(Modifier.fillMaxWidth()) }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(CeleDotyku.odstep)) { each(Modifier.fillMaxWidth()) }
    }
}
