/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import android.graphics.Bitmap
import io.github.dostepnykampus.core.designsystem.severityImage
import io.github.dostepnykampus.core.model.Severity
import kotlin.math.ceil

/**
 * Obrazy barier: znak każdej uciążliwości i jej słowo z [MapImages.words] (US1.3, NFR-21). Znak ma kształt
 * według uciążliwości w barwach `tusz` i `papier`, jak znaki wejść; słowo wygląda jak podpis budynku. Bez słów
 * są same znaki.
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
    return signs + words
}
