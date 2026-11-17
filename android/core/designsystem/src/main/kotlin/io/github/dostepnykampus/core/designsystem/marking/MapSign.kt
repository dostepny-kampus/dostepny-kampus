/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import io.github.dostepnykampus.core.designsystem.MapColors

private const val SIGN_GRID = 24f
private const val HALO_WIDTH = 4.2f
private const val OUTLINE_WIDTH = 1.4f

/**
 * Znak mapy jako obraz [sizePx] × [sizePx]: kształt [shape] z siatki 24 × 24 wypełniony barwą [fill], w konturze
 * `tusz` i obwódce `papier` z [colors], żeby odcinał się od każdego podkładu. Symbol w kształcie rysuje [symbol]
 * na tej samej siatce. Obwódka mieści się w obrazie.
 *
 * Wymagania: NFR-21, NFR-16
 */
internal fun mapSignBitmap(
    sizePx: Int,
    shape: Path,
    fill: Color,
    colors: MapColors,
    symbol: DrawScope.() -> Unit,
): Bitmap = signBitmap(sizePx) { drawMapSign(shape = shape, fill = fill, colors = colors, symbol = symbol) }

/** Znak mapy jak w [mapSignBitmap], narysowany na krótszym boku [DrawScope]. Wymagania: NFR-21, NFR-16 */
internal fun DrawScope.drawMapSign(shape: Path, fill: Color, colors: MapColors, symbol: DrawScope.() -> Unit) {
    scale(scale = size.minDimension / (SIGN_GRID + HALO_WIDTH), pivot = Offset.Zero) {
        translate(left = HALO_WIDTH / 2, top = HALO_WIDTH / 2) {
            drawPath(path = shape, color = colors.papier, style = Stroke(HALO_WIDTH, join = StrokeJoin.Round))
            drawPath(path = shape, color = fill)
            drawPath(path = shape, color = colors.tusz, style = Stroke(OUTLINE_WIDTH, join = StrokeJoin.Round))
            symbol()
        }
    }
}
