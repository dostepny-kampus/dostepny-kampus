/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

/**
 * Ikona braku sieci: fale Wi-Fi z ukośną kreską z Material Icons, siatka 24 × 24.
 */
public val NoInternetIcon: ImageVector =
    pathIcon(
        "M21 11l2-2c-3.73-3.73-8.87-5.15-13.7-4.31l2.58 2.58c3.3-.02 6.61 1.22 9.12 3.73zm-2 2c-1.08-1.08-2.36-1.85" +
            "-3.72-2.33l3.02 3.02.7-.69zM9 17l3 3 3-3c-1.65-1.66-4.34-1.66-6 0zM3.41 1.64L2 3.05 5.05 6.1C3.59 6.83 " +
            "2.22 7.79 1 9l2 2c1.23-1.23 2.65-2.16 4.17-2.78l2.24 2.24C7.79 10.89 6.27 11.74 5 13l2 2c1.35-1.35 3.11" +
            "-2.04 4.89-2.06l7.08 7.07 1.41-1.41L3.41 1.64z",
    )

/**
 * Zaokrąglona plakietka z ikoną i napisem „Bez internetu”, szeroka jak jej treść. Plakietka pojawia się
 * sama, więc TalkBack czyta ją jako komunikat stanu (WCAG 4.1.3).
 *
 * Wymagania: NFR-21, NFR-15
 *
 * @param modifier położenie plakietki wyznacza wołający
 */
@Composable
public fun OfflinePill(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(Obramowania.ramka, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Odstepy.m, vertical = Odstepy.s),
            horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(NoInternetIcon, contentDescription = null)
            Text(
                text = stringResource(R.string.bez_internetu),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = Typografia.wyroznienieWaga,
            )
        }
    }
}
