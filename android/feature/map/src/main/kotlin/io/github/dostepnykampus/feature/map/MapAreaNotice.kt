/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Wysokosci

/**
 * Napis „Pokazuję teren: Wittigowo” nad podpisem mapy według makiety hi-fi: mapa zmieniła teren, bo zaznaczony
 * budynek leży na terenie [area]. TalkBack czyta go sam, raz. Zostaje bez limitu czasu, dopóki
 * osoba nie ruszy mapy przyciskiem albo nie wybierze terenu.
 *
 * Wymagania: US1.8, NFR-18, NFR-23
 */
@Composable
internal fun AreaNotice(area: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(Odstepy.s),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = Wysokosci.kontrolkiMapy,
    ) {
        Text(
            text = stringResource(R.string.mapa_teren_pokazuje, area),
            modifier = Modifier.padding(Odstepy.m).semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
