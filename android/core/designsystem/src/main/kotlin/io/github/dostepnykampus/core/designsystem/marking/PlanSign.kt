/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import android.graphics.Bitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.model.room.RoomCategory

private const val SQUARE_START = 1f
private const val SQUARE_SIDE = 22f
private const val SQUARE_RADIUS = 5f

private const val PICTOGRAM_START = 2f
private const val PICTOGRAM_SIDE = 30f
private const val LINE_STROKE = 2.4f
private const val HEAD_X = 15f
private const val HEAD_Y = 9f
private const val HEAD_RADIUS = 2.4f
private const val LIFT_ARROWS = "M17 7l6 7H11zM17 27l6-7H11z"
private const val WHEELCHAIR_BODY = "M15 12.5v6h6l2.5 5.5M15 15h5"
private const val WHEELCHAIR_WHEEL = "M11.2 17.5a5 5 0 1 0 8.3 4.6"
private const val STAIRS = "M8 25h5v-4.5h5V16h5v-4.5h4"

private val PICTOGRAMS: Map<RoomCategory, DrawScope.(Color) -> Unit> =
    mapOf(
        RoomCategory.ELEVATOR to { color -> drawPath(path = parse(LIFT_ARROWS), color = color) },
        RoomCategory.ACCESSIBLE_TOILET to { color ->
            drawCircle(color = color, radius = HEAD_RADIUS, center = Offset(x = HEAD_X, y = HEAD_Y))
            drawStroke(path = WHEELCHAIR_BODY, color = color)
            drawStroke(path = WHEELCHAIR_WHEEL, color = color)
        },
        RoomCategory.STAIRCASE to { color -> drawStroke(path = STAIRS, color = color) },
    )

/**
 * Znak grupy funkcji [category] na planie piętra jako obraz [sizePx] × [sizePx]: zaokrąglony kwadrat w konturze
 * `tusz` i obwódce `papier` z [colors], jak znaki wejść i barier. Winda (strzałki) i toaleta dostosowana (wózek)
 * mają znak `papier` na kwadracie `tusz`, schody znak `tusz` na kwadracie `papier`. Inna grupa niż te trzy rzuca
 * [IllegalArgumentException].
 *
 * Wymagania: US16.1, NFR-16, NFR-21
 */
public fun mapPlanSignImage(category: RoomCategory, sizePx: Int, colors: MapColors): Bitmap {
    val pictogram = requireNotNull(PICTOGRAMS[category]) { "Grupa $category nie ma znaku planu" }
    val isStairs = category == RoomCategory.STAIRCASE
    val fill = if (isStairs) colors.papier else colors.tusz
    val symbol = if (isStairs) colors.tusz else colors.papier
    val end = SQUARE_START + SQUARE_SIDE
    val square =
        RoundRect(
            left = SQUARE_START,
            top = SQUARE_START,
            right = end,
            bottom = end,
            cornerRadius = CornerRadius(SQUARE_RADIUS),
        )
    val shape = Path().apply { addRoundRect(square) }
    return mapSignBitmap(sizePx = sizePx, shape = shape, fill = fill, colors = colors) {
        translate(left = SQUARE_START, top = SQUARE_START) {
            scale(scale = SQUARE_SIDE / PICTOGRAM_SIDE, pivot = Offset.Zero) {
                translate(left = -PICTOGRAM_START, top = -PICTOGRAM_START) { pictogram(symbol) }
            }
        }
    }
}

private fun parse(path: String): Path = PathParser().parsePathString(path).toPath()

private fun DrawScope.drawStroke(path: String, color: Color) {
    val line = Stroke(width = LINE_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(path = parse(path), color = color, style = line)
}
