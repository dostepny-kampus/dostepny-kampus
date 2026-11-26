/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

/**
 * Komunikat błędu: znak „!” w kole i pogrubiony tekst w kolorze błędu, ogłaszany przez czytnik ekranu.
 *
 * Wymagania: NFR-18, NFR-21
 *
 * @param text treść komunikatu
 * @param modifier położenie komunikatu wyznacza wołający
 */
@Composable
public fun ProblemText(text: String, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.error
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        ImportantSign(tint = color)
        Text(
            text = text,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            color = color,
            fontWeight = Typografia.wyroznienieWaga,
        )
    }
}
