/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.barrier

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.style.BARRIER_NO_FLOOR_DP
import io.github.dostepnykampus.feature.map.style.MapImages
import io.github.dostepnykampus.feature.map.style.barrierNoFloorImage
import io.github.dostepnykampus.feature.map.style.barrierSign
import io.github.dostepnykampus.feature.map.style.barrierSignImage
import io.github.dostepnykampus.feature.map.style.barrierWordImage
import io.github.dostepnykampus.feature.map.style.labelImage
import kotlin.math.ceil

/**
 * Obrazy barier: znak każdej uciążliwości z [barrierSign], jego wersja z dopiskiem „?” dla barier bez piętra
 * na planie i słowo z [MapImages.words]. Każda uciążliwość ma też większy znak wybranej bariery na kole `papier`
 * w obwódce `tusz`, w obu wersjach.
 *
 * Wymagania: US1.3, NFR-21, US1.5, US16.1, BR-14
 */
internal fun barrierImages(images: MapImages): Map<String, Bitmap> {
    val signs = Severity.entries.associate { barrierSignImage(it) to barrierSign(it, images.colors, images.density) }
    val noFloor = Severity.entries.associate { barrierNoFloorImage(it) to unknownFloorSign(it, images) }
    val words =
        Severity.entries.zip(images.words).associate { (severity, word) ->
            barrierWordImage(severity) to labelImage(word, images.colors, images.labelSize)
        }
    return signs + noFloor + words
}

private const val BADGE_DP = 17f
private const val BADGE_OFFSET_DP = 9.5f
private const val BADGE_GRID = 24f
private const val BADGE_HALO = 4.2f
private const val BADGE_CIRCLE_RADIUS = 11f
private const val BADGE_OUTLINE = 1.4f
private const val BADGE_MARK_STROKE = 2.6f
private const val QUESTION_PATH = "M9.6 9.6a2.5 2.5 0 1 1 3.4 2.3c-.6.3-1 .8-1 1.5v.4M12 16.6v.01"

private fun unknownFloorSign(severity: Severity, images: MapImages): Bitmap {
    val size = ceil(BARRIER_NO_FLOOR_DP * images.density).toInt()
    val sign = barrierSign(severity, images.colors, images.density)
    val image = createBitmap(size, size)
    val canvas = Canvas(image)
    val corner = (size - sign.width) / 2f
    canvas.drawBitmap(sign, corner, corner, null)
    val center = size / 2f
    val offset = BADGE_OFFSET_DP * images.density
    drawUnknownFloorBadge(
        canvas = canvas,
        centerX = center + offset,
        centerY = center - offset,
        badgePx = BADGE_DP * images.density,
        colors = images.colors,
    )
    return image
}

private fun drawUnknownFloorBadge(canvas: Canvas, centerX: Float, centerY: Float, badgePx: Float, colors: MapColors) {
    val scale = badgePx / (BADGE_GRID + BADGE_HALO)
    val gridCenter = BADGE_GRID / 2f
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.papier.toArgb() }
    val outline =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = BADGE_OUTLINE
            color = colors.tusz.toArgb()
        }
    val mark =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = BADGE_MARK_STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = colors.tusz.toArgb()
        }
    val question = PathParser().parsePathString(QUESTION_PATH).toPath().asAndroidPath()
    canvas.withTranslation(x = centerX, y = centerY) {
        scale(scale, scale)
        translate(-gridCenter, -gridCenter)
        drawCircle(gridCenter, gridCenter, BADGE_CIRCLE_RADIUS + BADGE_HALO / 2f, fill)
        drawCircle(gridCenter, gridCenter, BADGE_CIRCLE_RADIUS, outline)
        drawPath(question, mark)
    }
}
