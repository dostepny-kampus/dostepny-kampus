/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.StatusIcons
import io.github.dostepnykampus.core.designsystem.Zaokraglenia

/**
 * Czynność karty pozycji listy ([ListItemCard]) z napisem [text]: przycisk obrysowany szeroki jak napis, wysokość
 * co najmniej [CeleDotyku.minimum] i rośnie z tekstem, kształt [Zaokraglenia.kapsula], ramka [Obramowania.ramka]
 * w kolorze `outline`, napis `labelLarge` w kolorze `primary`. Przy [opensScreen] po napisie stoi strzałka
 * [StatusIcons.Open] bez opisu.
 *
 * Wymagania: NFR-16, NFR-17, NFR-20, NFR-21
 */
@Composable
public fun CardAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, opensScreen: Boolean = false) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = CeleDotyku.minimum),
        shape = RoundedCornerShape(Zaokraglenia.kapsula),
        border = BorderStroke(Obramowania.ramka, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
    ) {
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
