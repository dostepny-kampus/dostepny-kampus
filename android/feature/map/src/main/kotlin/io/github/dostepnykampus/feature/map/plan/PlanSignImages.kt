/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import android.graphics.Bitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.feature.map.style.PLAN_SIGN_CATEGORIES
import io.github.dostepnykampus.feature.map.style.PLAN_SIGN_DP
import io.github.dostepnykampus.feature.map.style.planSignImage
import kotlin.math.ceil

private const val SIGN_UNITS = 34f
private const val FRAME_INSET = 2f
private const val FRAME_SIDE = 30f
private const val FRAME_RADIUS = 7f
private const val FRAME_STROKE = 2.5f
private const val LINE_STROKE = 2.4f
private const val HEAD_X = 15f
private const val HEAD_Y = 9f
private const val HEAD_RADIUS = 2.4f
private const val LIFT_ARROWS = "M17 7l6 7H11zM17 27l6-7H11z"
private const val WHEELCHAIR_BODY = "M15 12.5v6h6l2.5 5.5M15 15h5"
private const val WHEELCHAIR_WHEEL = "M11.2 17.5a5 5 0 1 0 8.3 4.6"
private const val STAIRS = "M8 25h5v-4.5h5V16h5v-4.5h4"

/**
 * Obrazy znaków planu według makiety hi-fi „m-v1-mapa-plan”: winda i toaleta dostosowana jako znak `papier` na
 * kwadracie `tusz`, schody jako schodki `tusz` na kwadracie `papier` z ramką `tusz`; pary kontrastu z tokenów.
 * Bok znaku [PLAN_SIGN_DP] dp w gęstości [density].
 *
 * Wymagania: US16.1, NFR-16
 */
internal fun planSignImages(colors: MapColors, density: Float): Map<String, Bitmap> {
    val side = ceil(PLAN_SIGN_DP * density)
    return PLAN_SIGN_CATEGORIES.associate { category ->
        val image = ImageBitmap(width = side.toInt(), height = side.toInt())
        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(image),
            size = Size(width = side, height = side),
        ) {
            scale(scale = side / SIGN_UNITS, pivot = Offset.Zero) { drawSign(category, colors) }
        }
        planSignImage(category) to image.asAndroidBitmap()
    }
}

private fun DrawScope.drawSign(category: RoomCategory, colors: MapColors) {
    val isStairs = category == RoomCategory.STAIRCASE
    val back = if (isStairs) colors.papier else colors.tusz
    val front = if (isStairs) colors.tusz else colors.papier
    val corner = CornerRadius(FRAME_RADIUS)
    val frame = Offset(x = FRAME_INSET, y = FRAME_INSET)
    val square = Size(width = FRAME_SIDE, height = FRAME_SIDE)
    drawRoundRect(color = back, topLeft = frame, size = square, cornerRadius = corner)
    if (isStairs) {
        drawRoundRect(
            color = front,
            topLeft = frame,
            size = square,
            cornerRadius = corner,
            style = Stroke(FRAME_STROKE),
        )
        drawLine(path = STAIRS, color = front)
    } else if (category == RoomCategory.ELEVATOR) {
        drawPath(path = PathParser().parsePathString(LIFT_ARROWS).toPath(), color = front)
    } else {
        drawCircle(color = front, radius = HEAD_RADIUS, center = Offset(x = HEAD_X, y = HEAD_Y))
        drawLine(path = WHEELCHAIR_BODY, color = front)
        drawLine(path = WHEELCHAIR_WHEEL, color = front)
    }
}

private fun DrawScope.drawLine(path: String, color: Color) {
    val line = Stroke(width = LINE_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(path = PathParser().parsePathString(path).toPath(), color = color, style = line)
}
