/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.photo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.ImportantSign
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.feature.capture.R

/**
 * Ostrzeżenie związane z wykonywaniem zdjęć. [content] podaje treść ostrzeżenia.
 *
 * Wymagania: NFR-7, NFR-21, NFR-25
 */
@Composable
internal fun PhotoWarning(content: @Composable ColumnScope.() -> Unit) {
    val color = MaterialTheme.colorScheme.error
    OutlinedCard(modifier = Modifier.fillMaxWidth(), border = BorderStroke(Obramowania.ramka, color)) {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.xs)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ImportantSign(tint = color)
                Text(
                    text = stringResource(R.string.formularz_wazne),
                    color = color,
                    fontWeight = Typografia.wyroznienieWaga,
                )
            }
            content()
        }
    }
}
