/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.overlay

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.style.barrierSign
import io.github.dostepnykampus.feature.map.style.entranceSign

private val SampleWidth = 34.dp
private val SampleHeight = 26.dp
private val SampleInset = 4.dp
private val SampleStroke = 2.5.dp

private val ENTRANCE_WORDS =
    listOf(
        EntranceMarking.ACCESSIBLE to R.string.karta_wiersz_dostepne,
        EntranceMarking.INACCESSIBLE to R.string.mapa_legenda_wejscie_niedostepne,
        EntranceMarking.NO_DATA to R.string.mapa_legenda_wejscie_bez_danych,
    )

/**
 * Znaki mapy V1, każdy z napisem, do arkusza legendy. Znaki wejść i barier to te same obrazy co na mapie.
 *
 * Wymagania: US1.4, NFR-21, US1.3
 */
@Composable
internal fun LegendSigns() {
    LegendRow(stringResource(R.string.mapa_legenda_budynek)) { BuildingSample() }
    ENTRANCE_WORDS.forEach { (marking, word) ->
        LegendRow(stringResource(word)) {
            SignSample(marking) { colors, density -> entranceSign(marking, colors, density) }
        }
    }
    Severity.entries.forEach { severity ->
        LegendRow(stringResource(severity.nameRes())) {
            SignSample(severity) { colors, density -> barrierSign(severity, colors, density) }
        }
    }
}

/** Napis „przewiń” pod treścią arkusza, która się nie mieści. Wymagania: NFR-20 */
@Composable
internal fun ScrollHint() {
    Column {
        HorizontalDivider()
        Text(
            text = stringResource(R.string.mapa_legenda_przewin),
            modifier = Modifier.padding(horizontal = Odstepy.m, vertical = Odstepy.s),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun LegendRow(word: String, content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.m), verticalAlignment = Alignment.CenterVertically) {
        content()
        Text(text = word, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun BuildingSample() {
    val colors = DostepnyKampusTheme.mapColors
    Canvas(Modifier.size(width = SampleWidth, height = SampleHeight)) {
        val inset = SampleInset.toPx()
        val inner = Size(width = size.width - 2 * inset, height = size.height - 2 * inset)
        drawRect(color = colors.pwr, topLeft = Offset(inset, inset), size = inner)
        val outline = Stroke(SampleStroke.toPx())
        drawRect(color = colors.pwrKontur, topLeft = Offset(inset, inset), size = inner, style = outline)
    }
}

@Composable
private fun SignSample(sign: Any, draw: (MapColors, Float) -> Bitmap) {
    val colors = DostepnyKampusTheme.mapColors
    val density = LocalDensity.current.density
    val image = remember(key1 = sign, key2 = colors, key3 = density) { draw(colors, density) }
    Image(bitmap = image.asImageBitmap(), contentDescription = null, modifier = Modifier.width(SampleWidth))
}
