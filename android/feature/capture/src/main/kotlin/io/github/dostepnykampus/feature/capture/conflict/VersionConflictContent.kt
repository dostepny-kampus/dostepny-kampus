/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.conflict

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import io.github.dostepnykampus.core.data.submission.conflict.ConflictField
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.bar.PinnedButtonKind
import io.github.dostepnykampus.core.designsystem.marking.SeverityBadge
import io.github.dostepnykampus.core.designsystem.marking.barrierPlace
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.feature.capture.R

/**
 * Czynności C4 przy konflikcie wersji.
 *
 * @property onBack „Wstecz”; wybór czeka jak po „Zdecyduję później”
 * @property onKeepMine „Zostaw moją wersję”
 * @property onTakeServer „Weź wersję z serwera”
 * @property onLater „Zdecyduję później”
 */
data class ConflictActions(
    val onBack: () -> Unit,
    val onKeepMine: () -> Unit,
    val onTakeServer: () -> Unit,
    val onLater: () -> Unit,
)

/**
 * Widok C4 przy konflikcie wersji bez modelu widoku: napis o skutku, pola różnicy, obie wersje i wybór
 * na końcu treści. Poza [ConflictUiState.Ready] pokazuje sam nagłówek.
 *
 * Wymagania: US9.3, BR-10, PW-12, NFR-21, NFR-52
 *
 * @param state stan ekranu
 * @param actions czynności wyboru i powrotu
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun VersionConflictContent(state: ConflictUiState, actions: ConflictActions, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.konflikt_naglowek), onBack = actions.onBack)
            if (state is ConflictUiState.Ready) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m),
                    verticalArrangement = Arrangement.spacedBy(Odstepy.m),
                ) {
                    Effect(isAccepted = state.isAccepted)
                    Differences(state)
                    Version(title = R.string.konflikt_moja, content = state.conflict.own, state = state)
                    Version(title = R.string.konflikt_serwer, content = state.conflict.server.content, state = state)
                    Choices(actions)
                }
            }
        }
    }
}

@Composable
private fun Effect(isAccepted: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            Text(
                text = stringResource(R.string.konflikt_tytul),
                fontWeight = Typografia.wyroznienieWaga,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(text = stringResource(R.string.konflikt_skutek), style = MaterialTheme.typography.bodyLarge)
            if (isAccepted) {
                Text(text = stringResource(R.string.konflikt_przyjete), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun Differences(state: ConflictUiState.Ready) {
    val label = pluralStringResource(R.plurals.konflikt_rozni_sie, state.differences.size)
    val names = state.differences.map { stringResource(it) }.joinToString(separator = ", ")
    Text(
        text =
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = Typografia.wyroznienieWaga)) { append(label) }
                append(" ")
                append(names)
            },
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun Version(
    @StringRes title: Int,
    content: SubmissionContent,
    state: ConflictUiState.Ready,
) {
    val differs = state.conflict.differences
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            Text(
                text = stringResource(title),
                modifier = Modifier.semantics { heading() },
                fontWeight = Typografia.wyroznienieWaga,
                style = MaterialTheme.typography.titleLarge,
            )
            content.barrierType?.let(BarrierType::fromCode)?.let { type ->
                Fact(text = stringResource(type.nameRes()), isBold = ConflictField.BARRIER_TYPE in differs)
            }
            val place =
                barrierPlace(symbol = content.buildingSymbol, name = null, entrance = null, floor = content.floor)
            Fact(text = place, isBold = ConflictField.BUILDING in differs || ConflictField.FLOOR in differs)
            content.description?.let { Fact(text = it, isBold = ConflictField.DESCRIPTION in differs) }
            content.severity?.let(Severity::fromValue)?.let { severity ->
                Fact(text = stringResource(R.string.konflikt_uciazliwosc), isBold = ConflictField.SEVERITY in differs)
                SeverityBadge(severity)
            }
        }
    }
}

@Composable
private fun Fact(text: String, isBold: Boolean) {
    Text(
        text = text,
        fontWeight = if (isBold) Typografia.wyroznienieWaga else null,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun Choices(actions: ConflictActions) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Choice(text = R.string.konflikt_zostaw, onClick = actions.onKeepMine, kind = PinnedButtonKind.Tonal)
        Choice(text = R.string.konflikt_wez, onClick = actions.onTakeServer, kind = PinnedButtonKind.Tonal)
        Choice(text = R.string.konflikt_pozniej, onClick = actions.onLater, kind = PinnedButtonKind.Outlined)
    }
}

@Composable
private fun Choice(
    @StringRes text: Int,
    onClick: () -> Unit,
    kind: PinnedButtonKind,
) {
    PinnedButton(text = stringResource(text), onClick = onClick, modifier = Modifier.fillMaxWidth(), kind = kind)
}
