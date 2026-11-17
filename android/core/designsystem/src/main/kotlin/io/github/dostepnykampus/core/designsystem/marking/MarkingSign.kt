/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.iconSizeLikeText
import io.github.dostepnykampus.core.model.building.EntranceMarking

/** Znacznik testowy obrazu znaku w [MarkingSign]. */
internal const val MARKING_SIGN_TAG: String = "znak-oznaczenia"

/**
 * Oznaczenie wejścia w tekście: znak z mapy dla [marking] i słowo zwykłym tekstem, bez tła. Znak rośnie razem
 * z czcionką i nie ma własnej nazwy dla czytnika ekranu; czytane jest słowo.
 *
 * Wymagania: NFR-21, US2.1, US3.3, NFR-18, NFR-20
 *
 * @param marking oznaczenie, które wyznacza znak i słowo ze słownika
 * @param modifier modyfikator zewnętrzny
 * @param label własne słowo obok znaku; `null` daje słowo ze słownika oznaczenia
 */
@Composable
public fun MarkingSign(marking: EntranceMarking, modifier: Modifier = Modifier, label: String? = null) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SignImage(marking)
        Text(text = label ?: stringResource(marking.look().word), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SignImage(marking: EntranceMarking) {
    val density = LocalDensity.current
    val sizePx = with(density) { iconSizeLikeText().roundToPx() }
    val colors = DostepnyKampusTheme.mapColors
    val image =
        remember(marking, sizePx, colors) {
            mapMarkingImage(marking = marking, sizePx = sizePx, colors = colors).asImageBitmap()
        }
    Image(
        bitmap = image,
        contentDescription = null,
        modifier = Modifier.size(with(density) { sizePx.toDp() }).testTag(MARKING_SIGN_TAG),
    )
}
