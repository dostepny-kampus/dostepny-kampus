/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import io.github.dostepnykampus.core.model.EntranceMarking

private const val ICON_VIEWPORT = 24f

private const val MARK_STROKE = 2.6f

internal enum class MarkingLook(
    @param:StringRes val word: Int,
    val shape: String,
    val mark: String,
    val container: (StatusColors) -> Color,
    val content: (StatusColors) -> Color,
) {
    ACCESSIBLE(
        R.string.oznaczenie_dostepne,
        "M12 1a11 11 0 1 0 0 22a11 11 0 1 0 0-22z",
        "M7 12.4l3.3 3.3L17 9",
        StatusColors::statusDostepneContainer,
        StatusColors::onStatusDostepneContainer,
    ),
    INACCESSIBLE(
        R.string.oznaczenie_niedostepne,
        "M22.72 16.44L16.44 22.72H7.56L1.28 16.44V7.56L7.56 1.28H16.44L22.72 7.56z",
        "M8.6 8.6l6.8 6.8M15.4 8.6l-6.8 6.8",
        StatusColors::statusNiedostepneContainer,
        StatusColors::onStatusNiedostepneContainer,
    ),
    NO_DATA(
        R.string.oznaczenie_brak_danych,
        "M12 1.5l10.5 10.5L12 22.5 1.5 12z",
        "M9.6 9.6a2.5 2.5 0 1 1 3.4 2.3c-.6.3-1 .8-1 1.5v.4M12 16.6v.01",
        StatusColors::statusBrakDanychContainer,
        StatusColors::onStatusBrakDanychContainer,
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
        drawPath(
            path = mark,
            color = markColor,
            style = Stroke(width = MARK_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
