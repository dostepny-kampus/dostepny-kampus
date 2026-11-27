/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.SheetCloseButton
import io.github.dostepnykampus.feature.map.overlay.entranceTitle

/**
 * Nagłówek karty V2 w trybie [mode]: symbol i nazwa z [card] albo miejsce na nie w trakcie wczytywania; poza
 * [CardMode.Direct] obok „Zamknij”, które wywołuje [onClose]. Pod nimi na całą szerokość wiersz o wejściach
 * z odpowiedzią.
 *
 * Wymagania: US2, US2.8, US3.1, NFR-17, NFR-18, NFR-53
 */
@Composable
internal fun CardHeader(symbol: String, card: BuildingCardUiState, mode: CardMode, onClose: () -> Unit) {
    val isCompact = mode == CardMode.Compact
    if (!isCompact && card !is BuildingCardUiState.Ready) return
    val ready = card as? BuildingCardUiState.Ready
    val isShown = ready != null || card.isWaiting(!isCompact)
    val space = if (ready == null) Modifier.alpha(0f).clearAndSetSemantics {} else Modifier
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            Box(Modifier.weight(1f).then(space)) { if (isShown) NameOrSpace(symbol = symbol, ready = ready) }
            if (mode != CardMode.Direct) SheetCloseButton(onClose = onClose)
        }
        if (isShown) Box(space) { EntrancesOrSpace(ready) }
    }
}

@Composable
private fun EntrancesOrSpace(ready: BuildingCardUiState.Ready?) {
    if (ready == null) {
        EntranceSummaryLine(EntranceSummary.NoData)
    } else {
        EntranceLines(summary = entranceSummary(ready.entrances), answer = visitAnswer(ready.entrances))
    }
}

@Composable
private fun NameOrSpace(symbol: String, ready: BuildingCardUiState.Ready?) {
    if (ready != null) {
        SymbolOverName(ready)
    } else {
        Column {
            Text(text = symbol, style = MaterialTheme.typography.headlineLarge)
            Text(text = " ", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun EntranceLines(summary: EntranceSummary, answer: VisitAnswer) {
    val line = answerLine(answer)
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        EntranceSummaryLine(summary)
        line?.let { sentence ->
            Text(
                text = sentence,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Zdanie pod wierszem o wejściach dla [answer]: wejście zalecane z szerokością drzwi, gdy jest w danych, i barierą
 * przy nim, albo liczba wejść bez dostępnego lub z blokadą; bez danych `null`.
 *
 * Wymagania: US3.1, US2.2, BR-25, BR-27
 */
@Composable
@ReadOnlyComposable
internal fun answerLine(answer: VisitAnswer): String? =
    when (answer) {
        is VisitAnswer.Accessible -> {
            val entrance = if (answer.place.isNotEmpty()) placeLine(answer) else nameLine(answer)
            answer.barrier?.let { severity ->
                val word = stringResource(severity.nameRes()).replaceFirstChar(Char::lowercaseChar)
                stringResource(R.string.karta_odpowiedz_bariera, entrance, word)
            } ?: entrance
        }

        is VisitAnswer.Blocked -> {
            if (answer.count == 1) {
                stringResource(R.string.karta_odpowiedz_jedyne_blokada)
            } else {
                stringResource(R.string.karta_odpowiedz_kazde_blokada, answer.count)
            }
        }

        is VisitAnswer.NoAccessible -> {
            if (answer.total == 1) {
                stringResource(R.string.karta_odpowiedz_jedyne)
            } else {
                stringResource(R.string.karta_odpowiedz_zadne, answer.total)
            }
        }

        VisitAnswer.NoData, is VisitAnswer.Unknown -> {
            null
        }
    }

@Composable
@ReadOnlyComposable
private fun nameLine(answer: VisitAnswer.Accessible): String {
    val name = entranceTitle(answer.name, isFromData = answer.isNameFromData)
    return if (answer.doorWidthCm > 0) {
        stringResource(R.string.karta_odpowiedz_zalecane_drzwi, name, answer.doorWidthCm)
    } else {
        stringResource(R.string.karta_odpowiedz_zalecane, name)
    }
}

@Composable
@ReadOnlyComposable
private fun placeLine(answer: VisitAnswer.Accessible): String {
    val place = answer.place.replaceFirstChar(Char::lowercaseChar)
    return if (answer.doorWidthCm > 0) {
        stringResource(R.string.karta_odpowiedz_polozenie_drzwi, place, answer.doorWidthCm)
    } else {
        stringResource(R.string.karta_odpowiedz_polozenie, place)
    }
}

@Composable
private fun EntranceSummaryLine(summary: EntranceSummary) {
    if (summary == EntranceSummary.NoData) {
        Text(
            text = stringResource(R.string.karta_wiersz_brak_danych),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        EntranceSummaryRow(summary)
    }
}

internal fun BuildingCardUiState.isWaiting(isFullScreen: Boolean): Boolean =
    this == BuildingCardUiState.Loading && !isFullScreen

@Composable
private fun SymbolOverName(ready: BuildingCardUiState.Ready) {
    val shown = ready.symbols.joinToString(separator = " / ")
    val spoken = spokenHeader(ready)
    val focus = remember { FocusRequester() }
    Column(
        modifier =
            Modifier
                .semantics(mergeDescendants = true) { contentDescription = spoken }
                .focusRequester(focus)
                .focusable(),
    ) {
        Text(text = shown, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        ready.name?.let { Text(text = it, style = MaterialTheme.typography.titleLarge) }
    }
    LaunchedEffect(focus) { withFrameMillis { focus.requestFocus() } }
}

@Composable
@ReadOnlyComposable
private fun spokenHeader(ready: BuildingCardUiState.Ready): String =
    if (ready.symbols.size > 1) {
        val joined = ready.symbols.joinToString(separator = ", ")
        ready.name?.let { stringResource(R.string.karta_czytaj_wiele, joined, it) }
            ?: stringResource(R.string.mapa_budynki, joined)
    } else {
        val single = ready.symbols.singleOrNull().orEmpty()
        ready.name?.let { stringResource(R.string.karta_czytaj, single, it) }
            ?: stringResource(R.string.mapa_budynek, single)
    }
