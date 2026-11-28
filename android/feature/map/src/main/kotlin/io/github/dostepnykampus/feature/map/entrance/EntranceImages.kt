/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.map.style.ENTRANCE_PIN_IMAGE
import io.github.dostepnykampus.feature.map.style.entranceMarkImage
import io.github.dostepnykampus.feature.map.style.entranceSign
import io.github.dostepnykampus.feature.map.style.labelImage
import io.github.dostepnykampus.feature.map.style.pointCaptionImage
import kotlin.math.ceil

private const val PIN_UNIT_DP = 2f
private const val PIN_WIDTH = 14f
private const val PIN_HEIGHT = 20f
private const val PIN_LEFT = 5f
private const val PIN_TOP = 2f
private const val PIN_BODY = "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"
private const val PIN_DOT = "M12 11.5a2.5 2.5 0 1 1 0-5a2.5 2.5 0 0 1 0 5z"

/**
 * Obrazy wejść niezależne od zaznaczenia: znaki wszystkich oznaczeń z [entranceSign] i pinezka zaznaczonego
 * wejścia. Pinezka ma jednostki 2 dp (14 × 20), a jej czubek stoi na dole obrazu, gdzie jest kotwica.
 *
 * Wymagania: US1.4, US2.1, US3.3, NFR-21
 */
internal fun entranceImages(colors: MapColors, density: Float): Map<String, Bitmap> {
    val marks = EntranceMarking.entries.associate { entranceMarkImage(it) to entranceSign(it, colors, density) }
    return marks + (ENTRANCE_PIN_IMAGE to pinImage(unitPx = density * PIN_UNIT_DP, colors = colors))
}

/** Obraz podpisu zaznaczonego punktu [label] o wysokości [labelSize]. */
internal fun pointCaption(label: String?, colors: MapColors, labelSize: Float): Map<String, Bitmap> =
    label?.let { mapOf(pointCaptionImage(it) to labelImage(it, colors, labelSize)) }.orEmpty()

private fun pinImage(unitPx: Float, colors: MapColors): Bitmap {
    val size = Size(width = ceil(PIN_WIDTH * unitPx), height = ceil(PIN_HEIGHT * unitPx))
    val image = ImageBitmap(width = size.width.toInt(), height = size.height.toInt())
    val body = PathParser().parsePathString(PIN_BODY).toPath()
    val dot = PathParser().parsePathString(PIN_DOT).toPath()
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(image),
        size = size,
    ) {
        scale(scale = unitPx, pivot = Offset.Zero) {
            translate(left = -PIN_LEFT, top = -PIN_TOP) {
                drawPath(path = body, color = colors.tusz)
                drawPath(path = dot, color = colors.papier)
            }
        }
    }
    return image.asAndroidBitmap()
}
