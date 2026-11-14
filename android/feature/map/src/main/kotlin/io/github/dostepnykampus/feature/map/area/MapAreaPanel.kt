/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.area

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Wysokosci
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.controls.MapControlIcons
import io.github.dostepnykampus.feature.map.controls.mapControlBorder
import io.github.dostepnykampus.feature.map.data.AreaChoice

private val AreaRowHeight = 56.dp
private const val TEN = 10
private const val HUNDRED = 100
private val FEW = 2..4
private val TEENS = 12..14

/**
 * Lista terenów do panelu „Teren na mapie”: pokazany pierwszy, pozostałe od najbliższego.
 *
 * Wymagania: US1.7
 *
 * @property choices tereny w kolejności listy
 */
@Immutable
internal data class AreaList(val choices: List<AreaChoice>)

/**
 * Panel „Teren na mapie” mapy V1 według makiety hi-fi: nagłówek z „Zamknij”, który się nie przewija, i lista
 * terenów. Wiersz terenu ma nazwę, pod nią np. „9 budynków · 2 km”, a pokazany teren „· wybrany” i znak „✓”.
 * Po otwarciu fokus stoi na nagłówku.
 *
 * Wymagania: US1.7, NFR-20
 *
 * @param areas lista terenów
 * @param onClose dotknięcie „Zamknij”
 * @param onChoose nazwa terenu wybranego na liście
 * @param modifier wymiary panelu
 */
@Composable
internal fun AreaPanel(
    areas: AreaList,
    onClose: () -> Unit,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Zaokraglenia.duze),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = Wysokosci.kontrolkiMapy,
        border = mapControlBorder(),
    ) {
        Column {
            AreaPanelHeader(onClose)
            HorizontalDivider()
            val list = Modifier.weight(weight = 1f, fill = false).verticalScroll(rememberScrollState())
            Column(list.padding(Odstepy.s)) {
                areas.choices.forEach { choice -> AreaRow(choice) { onChoose(choice.name) } }
            }
        }
    }
}

@Composable
private fun AreaPanelHeader(onClose: () -> Unit) {
    val header = remember { FocusRequester() }
    LaunchedEffect(header) { withFrameMillis { header.requestFocus() } }
    Row(Modifier.padding(start = Odstepy.m, end = Odstepy.s), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.mapa_teren_panel),
            modifier =
                Modifier
                    .weight(1f)
                    .focusRequester(header)
                    .focusable()
                    .semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedButton(onClick = onClose, modifier = Modifier.heightIn(min = CeleDotyku.minimum)) {
            Text(stringResource(R.string.mapa_zamknij))
        }
    }
}

/**
 * Forma słowa „budynek” dla liczby [count]: „1 budynek”, „2 budynki”, „5 budynków”, „12 budynków”, „22 budynki”.
 * Liczona tutaj, bo napisy aplikacji są po polsku przy każdym języku telefonu.
 */
internal fun buildingsWord(count: Int): Int {
    val isFew = count % TEN in FEW && count % HUNDRED !in TEENS
    return when {
        count == 1 -> R.string.mapa_teren_budynek
        isFew -> R.string.mapa_teren_budynki
        else -> R.string.mapa_teren_budynkow
    }
}

@Composable
private fun AreaRow(choice: AreaChoice, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val isShown = choice.kilometers == null
    val buildings = stringResource(buildingsWord(choice.buildings), choice.buildings)
    val place = choice.kilometers?.let { stringResource(R.string.mapa_teren_km, it) }
    val away = place ?: stringResource(R.string.mapa_teren_wybrany)
    val detail = stringResource(R.string.mapa_teren_opis, buildings, away)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = AreaRowHeight)
                .clip(RoundedCornerShape(Zaokraglenia.srednie))
                .background(if (isShown) scheme.primaryContainer else scheme.surfaceContainerLowest)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { selected = isShown }
                .padding(horizontal = Odstepy.m, vertical = Odstepy.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = choice.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isShown) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            )
        }
        if (isShown) Icon(imageVector = MapControlIcons.Check, contentDescription = null)
    }
}
