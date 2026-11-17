/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.NoInternetIcon
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.campusDay
import io.github.dostepnykampus.feature.map.R
import java.time.Instant

/**
 * Wskaźnik braku sieci na V1: jedna linia z ikoną i napisem „Bez internetu” na całą szerokość, pod górą ekranu
 * (PW-4). Gdy na mapie są dane pobrane z serwera, linia ma dopisek „dane z …” z dniem
 * pobrania najstarszych z nich; przy samych danych z pakietu aplikacji daty nie ma. Linia
 * pojawia się sama, więc TalkBack czyta ją jako komunikat stanu
 * (WCAG 4.1.3).
 *
 * Wymagania: US9.1, NFR-10, NFR-21, NFR-15
 *
 * @param syncedAt chwila pobrania najstarszych danych z serwera na mapie; `null` dla danych z pakietu aplikacji
 * @param modifier położenie linii wyznacza wołający
 */
@Composable
internal fun OfflineLine(syncedAt: Instant?, modifier: Modifier = Modifier) {
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column {
            Row(
                modifier = Modifier.windowInsetsPadding(insets).padding(horizontal = Odstepy.m, vertical = Odstepy.s),
                horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(NoInternetIcon, contentDescription = null)
                Text(
                    text =
                        syncedAt?.let { synced ->
                            val day = campusDay(synced)
                            stringResource(R.string.mapa_bez_internetu_dane_z, day)
                        } ?: stringResource(R.string.mapa_bez_internetu),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = Typografia.wyroznienieWaga,
                )
            }
            HorizontalDivider(thickness = Obramowania.ramka, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
