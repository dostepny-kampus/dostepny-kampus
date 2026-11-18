/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.GroupContainer
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.feature.map.R

private val NEXT_LINE_PEEK_HEIGHT = 24.dp

/** Czy pod treścią karty wystaje nagłówek „Wejścia”: tylko na niskiej karcie ([isFullScreen] fałsz) z budynkiem. */
internal fun BuildingCardUiState.hasEntrancesPeek(isFullScreen: Boolean): Boolean =
    !isFullScreen && this != BuildingCardUiState.Missing

/** Wcięcie treści karty; bez dolnego, gdy wystaje nagłówek „Wejścia” ([isPeek]), więc wiersz ucina brzeg karty. */
internal fun Modifier.cardPadding(isPeek: Boolean): Modifier =
    padding(start = Odstepy.m, top = Odstepy.m, end = Odstepy.m, bottom = if (isPeek) 0.dp else Odstepy.m)

/**
 * Początek pełnego opisu pod kreską niskiej karty: cały nagłówek grupy „Wejścia” i ucięty następny wiersz, ta sama
 * wysokość w każdym stanie [card], także przed wczytaniem. Dotknięcie gotowej karty to [onExpand]; TalkBack czyta
 * całość jako przycisk „Wejścia”, bez uciętego wiersza.
 *
 * Wymagania: US2.8, NFR-17, NFR-18, NFR-53
 */
@Composable
internal fun EntrancesPeek(card: BuildingCardUiState, onExpand: () -> Unit) {
    val title = stringResource(R.string.karta_wejscia)
    val lineHeight = MaterialTheme.typography.titleLarge.lineHeight
    val header = with(LocalDensity.current) { lineHeight.toDp() }
    val shown = Odstepy.l + header + Odstepy.s + NEXT_LINE_PEEK_HEIGHT
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (card is BuildingCardUiState.Ready) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onExpand)
                        .semantics { contentDescription = title },
            ) {
                CutGroup(title = title, card = card, shown = shown)
            }
        } else {
            Box(Modifier.height(shown))
        }
    }
}

@Composable
private fun CutGroup(title: String, card: BuildingCardUiState.Ready, shown: Dp) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(shown)
                .clipToBounds()
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .clearAndSetSemantics {},
    ) {
        GroupContainer(title = title) {
            val first = card.entrances.firstOrNull()
            if (first == null) {
                Text(text = stringResource(R.string.karta_wejscia_brak), style = MaterialTheme.typography.bodyLarge)
            } else {
                EntranceRow(listed = first, withAssessment = false, onShowEntrance = null, onReportEntrance = null)
            }
        }
    }
}
