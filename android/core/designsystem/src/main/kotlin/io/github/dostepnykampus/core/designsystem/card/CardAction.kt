/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.card

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.submission.StatusIcons

/**
 * Czynność karty pozycji listy ([ListItemCard]) z napisem [text]: przycisk obrysowany ([OutlinedAction]) szeroki
 * jak napis, napis `labelLarge`. Przy [opensScreen] po napisie stoi strzałka [StatusIcons.Open] bez opisu.
 *
 * Wymagania: NFR-16, NFR-17, NFR-20, NFR-21
 */
@Composable
public fun CardAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, opensScreen: Boolean = false) {
    OutlinedAction(onClick = onClick, modifier = modifier) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
        if (opensScreen) {
            Icon(
                imageVector = StatusIcons.Open,
                contentDescription = null,
                modifier = Modifier.padding(start = Odstepy.s),
            )
        }
    }
}
