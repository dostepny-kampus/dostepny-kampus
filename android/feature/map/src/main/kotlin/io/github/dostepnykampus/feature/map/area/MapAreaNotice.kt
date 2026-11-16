/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.area

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Wysokosci
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.mapControlBorder

/**
 * Napis „Pokazuję teren: Wittigowo · 2 km na południowy wschód” nad podpisem mapy według makiety hi-fi: mapa
 * zmieniła teren na [shift], bo zaznaczony budynek leży na tym terenie. Odległość i kierunek mówią, gdzie jest nowy
 * teren, bez patrzenia na ruch mapy. Napis jest komunikatem stanu tylko przy pierwszym pokazaniu ([isNew]).
 * Wejście na ekran i zejście z niego oddaje [onShow]. Zostaje bez limitu czasu.
 *
 * Wymagania: US1.8, NFR-18, NFR-23, NFR-56
 */
@Composable
internal fun AreaNotice(shift: AreaShift, isNew: Boolean, onShow: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val direction = stringResource(shift.direction.label)
    val description = stringResource(R.string.mapa_teren_pokazuje_opis, shift.name, shift.kilometers, direction)
    val isLive = remember { isNew }
    val shown by rememberUpdatedState(onShow)
    DisposableEffect(Unit) {
        shown(true)
        onDispose { shown(false) }
    }
    Surface(
        modifier = modifier.padding(Odstepy.s),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = Wysokosci.kontrolkiMapy,
        border = mapControlBorder(),
    ) {
        Text(
            text = noticeText(shift),
            modifier =
                Modifier.padding(Odstepy.m).semantics {
                    contentDescription = description
                    if (isLive) liveRegion = LiveRegionMode.Polite
                },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/**
 * Wysokość napisu [shift] razem z odstępami wokół na mapie szerokiej na [width], liczona bez rysowania napisu;
 * `0.dp` bez napisu.
 *
 * Wymagania: US2.8, NFR-53
 */
@Composable
internal fun rememberNoticeHeight(shift: AreaShift?, width: Dp): Dp {
    if (shift == null) return 0.dp
    val text = noticeText(shift)
    val style = MaterialTheme.typography.bodyLarge
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val around = (Odstepy.s + Odstepy.m) * 2
    return remember(text, style, width, density) {
        val maxWidth = with(density) { (width - around).roundToPx() }.coerceAtLeast(minimumValue = 0)
        val laid = measurer.measure(text = text, style = style, constraints = Constraints(maxWidth = maxWidth))
        with(density) { laid.size.height.toDp() } + around
    }
}

@Composable
private fun noticeText(shift: AreaShift): String =
    stringResource(R.string.mapa_teren_pokazuje, shift.name, shift.kilometers, stringResource(shift.direction.label))

private val CompassDirection.label: Int
    get() =
        when (this) {
            CompassDirection.North -> R.string.mapa_kierunek_polnoc
            CompassDirection.NorthEast -> R.string.mapa_kierunek_polnocny_wschod
            CompassDirection.East -> R.string.mapa_kierunek_wschod
            CompassDirection.SouthEast -> R.string.mapa_kierunek_poludniowy_wschod
            CompassDirection.South -> R.string.mapa_kierunek_poludnie
            CompassDirection.SouthWest -> R.string.mapa_kierunek_poludniowy_zachod
            CompassDirection.West -> R.string.mapa_kierunek_zachod
            CompassDirection.NorthWest -> R.string.mapa_kierunek_polnocny_zachod
        }
