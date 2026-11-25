/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.export

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.StaffProblem
import io.github.dostepnykampus.core.designsystem.StaffProblemMessage
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedActionBar
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.model.export.ExportFormat
import io.github.dostepnykampus.feature.inspection.R

/**
 * K3 „Eksport” ze stanem z modelu widoku.
 *
 * Wymagania: US13
 *
 * @param actions czynności prowadzące poza ekran
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel stan pobrania; w aplikacji z grafu Hilt
 */
@Composable
fun ExportScreen(actions: ExportActions, modifier: Modifier = Modifier, viewModel: ExportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ExportContent(
        state = state,
        actions = actions,
        onChoose = viewModel::choose,
        onDownload = viewModel::download,
        modifier = modifier,
    )
}

/**
 * Widok K3 bez modelu widoku: wybór formatu, przygotowanie pliku, wynik pobrania i stany wspólne.
 *
 * Wymagania: US13, US13.1, US13.2, US13.3, NFR-50, NFR-52
 *
 * @param state stan ekranu
 * @param actions czynności prowadzące poza ekran
 * @param onChoose wybór formatu
 * @param onDownload „Pobierz plik” i „Spróbuj ponownie”
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun ExportContent(
    state: ExportUiState,
    actions: ExportActions,
    onChoose: (ExportFormat) -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val problem = problemOf(state.stage)
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.eksport_naglowek), onBack = actions.onBack)
            if (problem != null) {
                val onAction = if (problem == StaffProblem.FORBIDDEN) actions.onLeave else onDownload
                StaffProblemMessage(problem = problem, onAction = onAction)
            } else {
                ExportBody(state = state, onChoose = onChoose, onDownload = onDownload)
            }
        }
    }
}

@Composable
private fun ColumnScope.ExportBody(state: ExportUiState, onChoose: (ExportFormat) -> Unit, onDownload: () -> Unit) {
    val isInContent = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m),
        verticalArrangement = Arrangement.spacedBy(Odstepy.m),
    ) {
        Message(state.stage)
        Formats(chosen = state.format, onChoose = onChoose)
        Text(text = stringResource(R.string.eksport_zawartosc), style = MaterialTheme.typography.bodyLarge)
        if (isInContent) DownloadButton(onDownload)
    }
    if (!isInContent) PinnedActionBar { DownloadButton(onDownload) }
}

private fun problemOf(stage: ExportStage): StaffProblem? =
    when (stage) {
        ExportStage.OFFLINE -> StaffProblem.OFFLINE
        ExportStage.SERVER_ERROR -> StaffProblem.SERVER
        ExportStage.FORBIDDEN -> StaffProblem.FORBIDDEN
        ExportStage.IDLE, ExportStage.LOADING, ExportStage.SAVED, ExportStage.EMPTY -> null
    }

@Composable
private fun DownloadButton(onDownload: () -> Unit) {
    PinnedButton(
        text = stringResource(R.string.eksport_pobierz),
        onClick = onDownload,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Message(stage: ExportStage) {
    val title =
        when (stage) {
            ExportStage.LOADING -> R.string.eksport_przygotowujemy
            ExportStage.SAVED -> R.string.eksport_zapisany
            ExportStage.EMPTY -> R.string.eksport_pusty
            ExportStage.IDLE, ExportStage.OFFLINE, ExportStage.SERVER_ERROR, ExportStage.FORBIDDEN -> null
        } ?: return
    Surface(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            Text(
                text = stringResource(title),
                fontWeight = Typografia.wyroznienieWaga,
                style = MaterialTheme.typography.titleLarge,
            )
            if (stage == ExportStage.EMPTY) {
                Text(text = stringResource(R.string.eksport_pusty_plik), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun Formats(chosen: ExportFormat, onChoose: (ExportFormat) -> Unit) {
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(
            text = stringResource(R.string.eksport_format),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        ExportFormat.entries.forEach { format ->
            FormatOption(label = labelOf(format), isChosen = format == chosen) { onChoose(format) }
        }
    }
}

@StringRes
private fun labelOf(format: ExportFormat): Int =
    when (format) {
        ExportFormat.GEOJSON -> R.string.eksport_geojson
        ExportFormat.CSV -> R.string.eksport_csv
    }

@Composable
private fun FormatOption(
    @StringRes label: Int,
    isChosen: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().selectable(selected = isChosen, role = Role.RadioButton, onClick = onClick),
        border =
            BorderStroke(
                width = if (isChosen) Obramowania.wybrane else Obramowania.ramka,
                color = MaterialTheme.colorScheme.outline,
            ),
    ) {
        Row(modifier = Modifier.padding(Odstepy.m), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isChosen, onClick = null)
            Text(
                text = stringResource(label),
                modifier = Modifier.padding(start = Odstepy.s),
                fontWeight = Typografia.wyroznienieWaga,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
