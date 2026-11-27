/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.bar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.R
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.designsystem.iconSizeLikeText
import io.github.dostepnykampus.core.designsystem.pathIcon

private val MirroredArrowBack: ImageVector = arrowBack()

private fun arrowBack(): ImageVector =
    pathIcon(
        path = "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z",
        name = "ArrowBack",
        autoMirror = true,
    )

/**
 * Nagłówek ekranu: przycisk „Wstecz” (strzałka ← i słowo, zawsze w tym samym miejscu) wywołujący [onBack] i tytuł
 * [title] oznaczony dla TalkBack jako nagłówek. Strzałka `arrow_back` jest ozdobna i odwraca się w układzie od prawej
 * do lewej. Przycisk obrysowany ma wysokość co najmniej [CeleDotyku.minimum]. Przy zwykłej czcionce tytuł stoi obok
 * przycisku i zawija się w swojej kolumnie, a od skali czcionki 1,5 stoi pod nim na całą szerokość.
 *
 * [horizontalPadding] ustala odstęp przycisku i tytułu od bocznych krawędzi.
 * Wymagania: NFR-15, NFR-20, NFR-17
 */
@Composable
public fun BackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = Odstepy.m,
) {
    val isTitleBelow = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val titleText = @Composable { titleModifier: Modifier ->
        val asHeading = titleModifier.semantics { heading() }
        Text(text = title, modifier = asHeading, style = MaterialTheme.typography.titleLarge)
    }
    Column(modifier = modifier) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = Odstepy.s),
                horizontalArrangement = Arrangement.spacedBy(Odstepy.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onBack = onBack)
                if (!isTitleBelow) titleText(Modifier.weight(1f))
            }
        }
        HorizontalDivider(thickness = Obramowania.ramka, color = MaterialTheme.colorScheme.outlineVariant)
        val below = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding).padding(top = Odstepy.m)
        if (isTitleBelow) titleText(below)
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    OutlinedButton(
        onClick = onBack,
        modifier = Modifier.heightIn(min = CeleDotyku.minimum),
        shape = RoundedCornerShape(Zaokraglenia.kapsula),
        border = BorderStroke(Obramowania.ramka, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Icon(
            imageVector = MirroredArrowBack,
            contentDescription = null,
            modifier = Modifier.size(iconSizeLikeText()),
        )
        Spacer(modifier = Modifier.width(Odstepy.s))
        Text(
            text = stringResource(R.string.wstecz),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = Typografia.wyroznienieWaga,
        )
    }
}
