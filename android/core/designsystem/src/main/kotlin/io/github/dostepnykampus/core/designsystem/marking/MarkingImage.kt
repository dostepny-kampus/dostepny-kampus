/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
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
import io.github.dostepnykampus.core.designsystem.R
import io.github.dostepnykampus.core.designsystem.StatusColors
import io.github.dostepnykampus.core.model.building.EntranceMarking

private const val ICON_VIEWPORT = 24f

private const val MARK_STROKE = 2.6f

/**
 * Kształty i barwy znaku oznaczenia wejścia na siatce 24 × 24.
 *
 * Wymagania: NFR-21, US2.1
 *
 * @property word słowo oznaczenia
 * @property shape wypełniony kształt: koło, ośmiokąt albo romb
 * @property mark kreski w kształcie: ptaszek, krzyżyk albo znak zapytania
 * @property container barwa kształtu w treści
 * @property content barwa kresek w treści
 * @property mapFill barwa kształtu na mapie i w legendzie
 * @property mapSymbol barwa kresek na mapie i w legendzie
 */
internal enum class MarkingLook(
    @param:StringRes val word: Int,
    val shape: String,
    val mark: String,
    val container: (StatusColors) -> Color,
    val content: (StatusColors) -> Color,
    val mapFill: (MapColors) -> Color,
    val mapSymbol: (MapColors) -> Color,
) {
    ACCESSIBLE(
        R.string.oznaczenie_dostepne,
        "M12 1a11 11 0 1 0 0 22a11 11 0 1 0 0-22z",
        "M7 12.4l3.3 3.3L17 9",
        StatusColors::statusDostepneContainer,
        StatusColors::onStatusDostepneContainer,
        MapColors::znakDostepne,
        MapColors::znakDostepneSymbol,
    ),
    INACCESSIBLE(
        R.string.oznaczenie_niedostepne,
        "M22.72 16.44L16.44 22.72H7.56L1.28 16.44V7.56L7.56 1.28H16.44L22.72 7.56z",
        "M8.6 8.6l6.8 6.8M15.4 8.6l-6.8 6.8",
        StatusColors::statusNiedostepneContainer,
        StatusColors::onStatusNiedostepneContainer,
        MapColors::tusz,
        MapColors::papier,
    ),
    NO_DATA(
        R.string.oznaczenie_brak_danych,
        "M12 1.5l10.5 10.5L12 22.5 1.5 12z",
        "M9.6 9.6a2.5 2.5 0 1 1 3.4 2.3c-.6.3-1 .8-1 1.5v.4M12 16.6v.01",
        StatusColors::statusBrakDanychContainer,
        StatusColors::onStatusBrakDanychContainer,
        MapColors::papier,
        MapColors::tusz,
    ),
}

internal fun EntranceMarking.look(): MarkingLook =
    when (this) {
        EntranceMarking.ACCESSIBLE -> MarkingLook.ACCESSIBLE
        EntranceMarking.INACCESSIBLE -> MarkingLook.INACCESSIBLE
        EntranceMarking.NO_DATA -> MarkingLook.NO_DATA
    }

/** Znak oznaczenia w skali 24 jednostek na krótszy bok [DrawScope]: kształt [shape] z znakiem [mark] w środku. */
internal fun DrawScope.drawMarking(shape: Path, mark: Path, shapeColor: Color, markColor: Color) {
    scale(scale = size.minDimension / ICON_VIEWPORT, pivot = Offset.Zero) {
        drawPath(path = shape, color = shapeColor)
        drawMarkingMark(mark = mark, color = markColor)
    }
}

private fun DrawScope.drawMarkingMark(mark: Path, color: Color) {
    val line = Stroke(width = MARK_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(path = mark, color = color, style = line)
}

/**
 * Znak oznaczenia [marking] jako obraz [sizePx] × [sizePx] dla mapy i jej legendy, z tych samych kształtów co
 * [MarkingChip], w konturze `tusz` i obwódce `papier` z [colors]. „Dostępne” ma barwy `znak_dostepne`,
 * „Niedostępne” kształt `tusz` ze znakiem `papier`, a „Brak danych” jest cichym znakiem: wnętrze `papier`,
 * znak `tusz`.
 *
 * Wymagania: US2.1, US1.4, NFR-21, NFR-16
 */
public fun mapMarkingImage(marking: EntranceMarking, sizePx: Int, colors: MapColors): Bitmap {
    val look = marking.look()
    val shape = PathParser().parsePathString(look.shape).toPath()
    val mark = PathParser().parsePathString(look.mark).toPath()
    return mapSignBitmap(sizePx = sizePx, shape = shape, fill = look.mapFill(colors), colors = colors) {
        drawMarkingMark(mark = mark, color = look.mapSymbol(colors))
    }
}

/** Obraz [sizePx] × [sizePx] ze znakiem narysowanym przez [draw], dla warstw mapy. */
internal fun signBitmap(sizePx: Int, draw: DrawScope.() -> Unit): Bitmap {
    val image = ImageBitmap(width = sizePx, height = sizePx)
    val size = Size(width = sizePx.toFloat(), height = sizePx.toFloat())
    CanvasDrawScope().draw(
        density = Density(density = 1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(image),
        size = size,
        block = draw,
    )
    return image.asAndroidBitmap()
}
