/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.barrier

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import io.github.dostepnykampus.core.designsystem.severityImage
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.feature.map.MapImages
import io.github.dostepnykampus.feature.map.labelImage
import io.github.dostepnykampus.feature.map.style.BARRIER_SIGN_DP
import io.github.dostepnykampus.feature.map.style.CHOSEN_BARRIER_DP
import io.github.dostepnykampus.feature.map.style.barrierSignImage
import io.github.dostepnykampus.feature.map.style.barrierWordImage
import io.github.dostepnykampus.feature.map.style.chosenBarrierImage
import kotlin.math.ceil

/**
 * Obrazy barier: znak każdej uciążliwości i jej słowo z [MapImages.words]. Znak ma kształt
 * według uciążliwości w barwach `tusz` i `papier`, jak znaki wejść; słowo wygląda jak podpis budynku. Bez słów
 * są same znaki. Każda uciążliwość ma też znak wybranej bariery: większy znak na kole `papier` w obwódce
 * `tusz`.
 *
 * Wymagania: US1.3, NFR-21, US1.5
 */
internal fun barrierImages(images: MapImages): Map<String, Bitmap> {
    val size = ceil(BARRIER_SIGN_DP * images.density).toInt()
    val signs =
        Severity.entries.associate { severity ->
            val sign =
                severityImage(
                    severity = severity,
                    sizePx = size,
                    shapeColor = images.colors.tusz,
                    markColor = images.colors.papier,
                )
            barrierSignImage(severity) to sign
        }
    val words =
        Severity.entries.zip(images.words).associate { (severity, word) ->
            barrierWordImage(severity) to labelImage(word, images.colors, images.labelSize)
        }
    val chosen = Severity.entries.associate { chosenBarrierImage(it) to chosenSign(it, images) }
    return signs + words + chosen
}

private const val CHOSEN_RING_DP = 3
private const val CHOSEN_SIGN_DP = 40

private fun chosenSign(severity: Severity, images: MapImages): Bitmap {
    val size = ceil(CHOSEN_BARRIER_DP * images.density).toInt()
    val signSize = ceil(CHOSEN_SIGN_DP * images.density).toInt()
    val ring = CHOSEN_RING_DP * images.density
    val sign =
        severityImage(
            severity = severity,
            sizePx = signSize,
            shapeColor = images.colors.tusz,
            markColor = images.colors.papier,
        )
    val image = createBitmap(size, size)
    val canvas = Canvas(image)
    val center = size / 2f
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = images.colors.papier.toArgb() }
    val outline =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = ring
            color = images.colors.tusz.toArgb()
        }
    canvas.drawCircle(center, center, center - ring, fill)
    canvas.drawCircle(center, center, center - ring, outline)
    val corner = (size - signSize) / 2f
    canvas.drawBitmap(sign, corner, corner, null)
    return image
}
