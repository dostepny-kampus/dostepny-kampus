/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import android.graphics.Bitmap
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.designsystem.marking.mapPlanSignImage
import io.github.dostepnykampus.feature.map.style.PLAN_SIGN_CATEGORIES
import io.github.dostepnykampus.feature.map.style.PLAN_SIGN_DP
import io.github.dostepnykampus.feature.map.style.planSignImage
import kotlin.math.ceil

/**
 * Obrazy znaków windy, toalety dostosowanej i schodów na planie z [mapPlanSignImage], w barwach [colors], po nazwie
 * z [planSignImage]. Bok obrazu to największy rozmiar znaku, [PLAN_SIGN_DP] dp w gęstości [density]; mniejsze
 * rozmiary daje warstwa mapy.
 *
 * Wymagania: US16.1, NFR-16, NFR-21
 */
internal fun planSignImages(colors: MapColors, density: Float): Map<String, Bitmap> {
    val side = ceil(PLAN_SIGN_DP * density).toInt()
    return PLAN_SIGN_CATEGORIES.associate { category ->
        planSignImage(category) to mapPlanSignImage(category = category, sizePx = side, colors = colors)
    }
}
