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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.createBitmap
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.core.model.submission.Severity
import kotlin.math.ceil

private const val LABEL_HALO_PER_TEXT = 0.25f

/** Obraz podpisu konturu: tekst `napis_pwr` z obwódką `obwodka` (para kontrastu z tokenów). Wymagania: NFR-16 */
internal fun labelImage(label: String, colors: MapColors, size: Float): Bitmap =
    textImage(label = label, text = colors.napisPwr, halo = colors.obwodka, size = size)

/** Obraz numeru sali na planie: tekst `napis_ulica` z obwódką `obwodka` (para kontrastu). Wymagania: US16.1 */
internal fun roomNumberBitmap(number: String, colors: MapColors, size: Float): Bitmap =
    textImage(label = number, text = colors.napisUlica, halo = colors.obwodka, size = size)

private fun textImage(label: String, text: Color, halo: Color, size: Float): Bitmap {
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
    listOf(Paint.Style.STROKE to halo, Paint.Style.FILL to text).forEach { (style, color) ->
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

private const val ROOM_STEP_SIZE_DP = 32f
private const val ROOM_STEP_NUMBER_DP = 16f
private const val ROOM_STEP_COUNT = 3

/** Obrazy numerów kroków do sali, po jednej wersji dla obu stanów. Wymagania: US18.2, NFR-21 */
internal fun roomStepImages(images: MapImages): Map<String, Bitmap> =
    buildMap {
        repeat(ROOM_STEP_COUNT) { index ->
            listOf(false, true).forEach { selected ->
                val key = roomStepImage(index = index, selected = selected)
                put(key, roomStepBitmap(number = index + 1, selected = selected, images = images))
            }
        }
    }

private fun roomStepBitmap(number: Int, selected: Boolean, images: MapImages): Bitmap {
    val density = images.density
    val size = ceil(ROOM_STEP_SIZE_DP * density).toInt()
    val center = size / 2f
    val radius = center - density
    val colors = images.colors
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = (if (selected) colors.pwrKontur else colors.obwodka).toArgb()
    canvas.drawCircle(center, center, radius, paint)
    paint.color = colors.napisPwr.toArgb()
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = density
    canvas.drawCircle(center, center, radius, paint)
    paint.style = Paint.Style.FILL
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = ROOM_STEP_NUMBER_DP * density
    paint.color = (if (selected) colors.obwodka else colors.napisPwr).toArgb()
    canvas.drawText(number.toString(), center, center - (paint.ascent() + paint.descent()) / 2f, paint)
    return bitmap
}
