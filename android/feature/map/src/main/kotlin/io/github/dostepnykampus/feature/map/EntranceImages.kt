/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

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
import io.github.dostepnykampus.core.designsystem.markingImage
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.feature.map.data.SelectedEntrance
import kotlin.math.ceil

private const val PIN_UNIT_DP = 2f
private const val PIN_WIDTH = 14f
private const val PIN_HEIGHT = 20f
private const val PIN_LEFT = 5f
private const val PIN_TOP = 2f
private const val PIN_BODY = "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"
private const val PIN_DOT = "M12 11.5a2.5 2.5 0 1 1 0-5a2.5 2.5 0 0 1 0 5z"

/**
 * Obrazy wejść: znaki wszystkich oznaczeń, a dla [selected] także pinezka i podpis z nazwą (US2.1, US3.3).
 * Pinezka ma jednostki 2 dp (14 × 20), a jej czubek stoi na dole obrazu, gdzie jest kotwica.
 */
internal fun entranceImages(
    selected: SelectedEntrance?,
    colors: MapColors,
    density: Float,
    labelSize: Float,
): Map<String, Bitmap> {
    val marks =
        EntranceMarking.entries.associate { marking ->
            val image =
                markingImage(
                    marking = marking,
                    sizePx = ceil(ENTRANCE_MARK_DP * density).toInt(),
                    shapeColor = colors.tusz,
                    markColor = colors.papier,
                )
            entranceMarkImage(marking) to image
        }
    val chosen =
        selected?.let { entrance ->
            val pin = pinImage(unitPx = density * PIN_UNIT_DP, colors = colors)
            mapOf(
                ENTRANCE_PIN_IMAGE to pin,
                entranceCaptionImage(entrance.label) to labelImage(entrance.label, colors, labelSize),
            )
        }
    return marks + chosen.orEmpty()
}

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
