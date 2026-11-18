/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import android.icu.text.PluralRules
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.GroupContainer
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.marking.MarkingSign
import io.github.dostepnykampus.core.designsystem.pathIcon
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.map.R
import java.util.Locale

private val NEXT_LINE_PEEK_HEIGHT = 24.dp

private val PolishPlurals: PluralRules = PluralRules.forLocale(Locale.forLanguageTag("pl"))

private val ChevronRight: ImageVector =
    pathIcon(path = "M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z", name = "ChevronRight", autoMirror = true)

/** Czy pod treścią karty wystaje nagłówek „Wejścia”: tylko na niskiej karcie ([isFullScreen] fałsz) z budynkiem. */
internal fun BuildingCardUiState.hasEntrancesPeek(isFullScreen: Boolean): Boolean =
    !isFullScreen && this != BuildingCardUiState.Missing

/**
 * Wcięcie treści karty; bez dolnego, gdy wystaje nagłówek „Wejścia” ([isPeek]), więc wiersz ucina brzeg karty,
 * i bez górnego, gdy nad treścią stoi nieprzewijany nagłówek (`false` w [hasTop]).
 */
internal fun Modifier.cardPadding(isPeek: Boolean, hasTop: Boolean = true): Modifier =
    padding(
        start = Odstepy.m,
        top = if (hasTop) Odstepy.m else 0.dp,
        end = Odstepy.m,
        bottom = if (isPeek) 0.dp else Odstepy.m,
    )

/**
 * Początek pełnego opisu niskiej karty: cały nagłówek grupy „Wejścia” i ucięty następny wiersz, ta sama
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
    if (card is BuildingCardUiState.Ready) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onExpand)
                    .semantics { contentDescription = title },
        ) {
            CutGroup(card = card, shown = shown)
        }
    } else {
        Box(Modifier.height(shown))
    }
}

@Composable
private fun CutGroup(card: BuildingCardUiState.Ready, shown: Dp) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(shown)
                .clipToBounds()
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .clearAndSetSemantics {},
    ) {
        EntrancesGroup(card = card, withAssessment = false, onShowEntrance = null, onReport = null)
    }
}

/**
 * Grupa „Wejścia” karty [card]: wejścia jedno pod drugim z linią między nimi, jeden wiersz dla kilku wejść bez
 * oznaczenia i nazwy albo zdanie o braku danych.
 *
 * Wymagania: US2.1, US3.3, US4.6, US17.1, NFR-17, NFR-18, NFR-21
 *
 * @param card gotowa karta budynku z wejściami w kolejności karty
 * @param withAssessment czy przy każdym wejściu stoi data i źródło oznaczenia
 * @param onShowEntrance dotknięcie wiersza z identyfikatorem wejścia; `null` daje wiersze bez dotyku
 * @param onReport „Zgłoś barierę przy wejściu” bez wybranego wejścia; `null` ukrywa odnośnik
 */
@Composable
internal fun EntrancesGroup(
    card: BuildingCardUiState.Ready,
    withAssessment: Boolean,
    onShowEntrance: ((String) -> Unit)?,
    onReport: (() -> Unit)?,
) {
    GroupContainer(title = stringResource(R.string.karta_wejscia)) {
        when {
            card.entrances.isEmpty() -> NoEntrances()
            card.hasOnlyNamelessEntrances() -> NamelessEntrances(count = card.entrances.size, onReport = onReport)
            else -> EntranceList(card = card, withAssessment = withAssessment, onShow = onShowEntrance)
        }
    }
}

/** Czy karta ma co najmniej dwa wejścia, a żadne nie ma oznaczenia ani nazwy w danych. Wymagania: US2.1, US17.1 */
internal fun BuildingCardUiState.Ready.hasOnlyNamelessEntrances(): Boolean =
    entrances.size > 1 && entrances.all { it.entrance.marking == EntranceMarking.NO_DATA && it.entrance.name == null }

@Composable
private fun EntranceList(card: BuildingCardUiState.Ready, withAssessment: Boolean, onShow: ((String) -> Unit)?) {
    val line = MaterialTheme.colorScheme.outlineVariant
    card.entrances.forEachIndexed { index, listed ->
        if (index > 0) HorizontalDivider(Modifier.padding(vertical = Odstepy.s), color = line)
        EntranceRow(listed = listed, withAssessment = withAssessment, onShowEntrance = onShow)
    }
}

@Composable
private fun NoEntrances() =
    Text(text = stringResource(R.string.karta_wejscia_brak), style = MaterialTheme.typography.bodyLarge)

@Composable
private fun NamelessEntrances(count: Int, onReport: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.xs)) {
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(Odstepy.xs),
        ) {
            MarkingSign(marking = EntranceMarking.NO_DATA)
            val isFew = PolishPlurals.select(count.toDouble()) == PluralRules.KEYWORD_FEW
            val form = if (isFew) R.string.karta_wejscia_bez_nazw_kilka else R.string.karta_wejscia_bez_nazw_wiele
            val title = stringResource(form, count)
            Text(text = title, fontWeight = Typografia.wyroznienieWaga, style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(R.string.karta_nikt_nie_sprawdzil), style = MaterialTheme.typography.bodyMedium)
        }
        onReport?.let { report ->
            TextButton(onClick = report, modifier = Modifier.heightIn(min = CeleDotyku.minimum)) {
                Text(stringResource(R.string.karta_zglos))
            }
        }
    }
}

/**
 * Wiersz wejścia na liście: oznaczenie, znacznik „zalecane”, nazwa i ocena; z [onShowEntrance] dotykalny w całości,
 * ze strzałką po prawej. Jeden układ dla grupy i jej wystającego początku, więc wygląd się nie rozjedzie.
 *
 * Wymagania: US2.1, US3.3, US17, NFR-17, NFR-18
 *
 * @param listed wejście z kolejnością i znacznikiem „zalecane”
 * @param withAssessment czy pokazać ocenę wejścia; bez niej, gdy wszystkie wejścia mają tę samą ocenę nad listą
 * @param onShowEntrance dotknięcie wiersza z identyfikatorem wejścia; `null` daje wiersz bez dotyku i strzałki
 */
@Composable
internal fun EntranceRow(listed: OrderedEntrance, withAssessment: Boolean, onShowEntrance: ((String) -> Unit)?) {
    val action = stringResource(R.string.wizyta_pokaz)
    val touch =
        onShowEntrance?.let { show ->
            Modifier.clickable(onClickLabel = action, role = Role.Button) { show(listed.entrance.id) }
        } ?: Modifier.semantics(mergeDescendants = true) {}
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.minimum).then(touch),
        horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntranceFacts(listed = listed, withAssessment = withAssessment, modifier = Modifier.weight(1f))
        if (onShowEntrance != null) Icon(imageVector = ChevronRight, contentDescription = null)
    }
}

@Composable
private fun EntranceFacts(listed: OrderedEntrance, withAssessment: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Odstepy.xs)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
            verticalArrangement = Arrangement.spacedBy(Odstepy.xs),
        ) {
            MarkingSign(marking = listed.entrance.marking)
            if (listed.isRecommended) {
                Text(
                    text = stringResource(R.string.karta_zalecane),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = Typografia.wyroznienieWaga,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Text(
            text = listed.entrance.name ?: stringResource(R.string.karta_bez_nazwy),
            fontWeight = Typografia.wyroznienieWaga,
            style = MaterialTheme.typography.titleMedium,
        )
        if (withAssessment) listed.entrance.assessment?.let { AssessmentLine(it) }
    }
}
