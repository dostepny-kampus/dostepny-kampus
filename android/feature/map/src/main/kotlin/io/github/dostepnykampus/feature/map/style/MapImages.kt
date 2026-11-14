/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.createBitmap
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.designsystem.nameRes
import io.github.dostepnykampus.core.model.Severity
import kotlin.math.ceil

private const val LABEL_HALO_PER_TEXT = 0.25f

/** Obraz podpisu konturu: tekst `napis_pwr` z obwódką `obwodka` (para kontrastu z tokenów). Wymagania: NFR-16 */
internal fun labelImage(label: String, colors: MapColors, size: Float): Bitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.textSize = size
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.strokeWidth = size * LABEL_HALO_PER_TEXT
    paint.strokeJoin = Paint.Join.ROUND
    val margin = paint.strokeWidth
    val width = ceil(paint.measureText(label) + 2 * margin).toInt()
    val height = ceil(paint.descent() - paint.ascent() + 2 * margin).toInt()
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    listOf(Paint.Style.STROKE to colors.obwodka, Paint.Style.FILL to colors.napisPwr).forEach { (style, color) ->
        paint.style = style
        paint.color = color.toArgb()
        canvas.drawText(label, margin, margin - paint.ascent(), paint)
    }
    return bitmap
}

/**
 * Wygląd obrazów mapy w barwach [colors], gęstości ekranu [density] i wysokości podpisu [labelSize] w pikselach,
 * ze słowami uciążliwości w języku ekranu; nowy tylko po zmianie któregoś z nich.
 */
@Composable
internal fun rememberMapImages(colors: MapColors, density: Float, labelSize: Float): MapImages {
    val words = Severity.entries.map { stringResource(it.nameRes()) }
    return remember(colors, density, labelSize, words) {
        MapImages(colors = colors, density = density, labelSize = labelSize, words = words)
    }
}

/**
 * Wygląd obrazów mapy: barwy [colors], gęstość ekranu [density], wysokość podpisu [labelSize] w pikselach i słowa
 * uciążliwości [words] w kolejności [Severity]; bez słów bariery mają same znaki.
 */
internal data class MapImages(
    val colors: MapColors,
    val density: Float,
    val labelSize: Float,
    val words: List<String> = emptyList(),
)
