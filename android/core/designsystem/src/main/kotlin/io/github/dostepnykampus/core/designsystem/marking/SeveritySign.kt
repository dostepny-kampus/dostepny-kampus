/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.designsystem.iconSizeLikeText
import io.github.dostepnykampus.core.model.submission.Severity

private const val SIGN_VIEWPORT = 24f

/**
 * Kształty znaku uciążliwości na siatce 24 × 24, jak w zatwierdzonym zestawie znaków mapy.
 *
 * Wymagania: NFR-21
 *
 * @property shape wypełniony kształt: trójkąt, zaokrąglona tarcza albo zaokrąglona kwadratowa tablica
 * @property mark kreski w kształcie: wykrzyknik albo ramiona osób; pusty napis, gdy ich brak
 * @property stroke grubość kresek; 0 bez kresek
 * @property fills wypełnione części w kolorze kresek: kropka wykrzyknika, głowy osób albo pozioma belka; pusty
 *   napis, gdy ich brak
 */
internal enum class SeverityLook(val shape: String, val mark: String, val stroke: Float, val fills: String) {
    HINDRANCE(
        shape = "M12 2.4l10.2 18.2H1.8z",
        mark = "M12 9.2v5.2",
        stroke = 2.3f,
        fills = "M12 16.25a1.25 1.25 0 1 0 0 2.5a1.25 1.25 0 1 0 0-2.5z",
    ),
    ONLY_WITH_HELP(
        shape = "M12 1.8l8.8 3.1v6.3c0 5.1-3.5 9.1-8.8 11-5.3-1.9-8.8-5.9-8.8-11V4.9z",
        mark = "M6.6 15.6c0-2 1-3.2 2.4-3.2s2.4 1.2 2.4 3.2M12.6 15.6c0-2 1-3.2 2.4-3.2s2.4 1.2 2.4 3.2",
        stroke = 1.9f,
        fills =
            "M9 7.05a1.75 1.75 0 1 0 0 3.5a1.75 1.75 0 1 0 0-3.5z" +
                "M15 7.05a1.75 1.75 0 1 0 0 3.5a1.75 1.75 0 1 0 0-3.5z",
    ),
    BLOCKING(
        shape =
            "M5.3 2h13.4a3.3 3.3 0 0 1 3.3 3.3v13.4a3.3 3.3 0 0 1-3.3 3.3H5.3" +
                "a3.3 3.3 0 0 1-3.3-3.3V5.3a3.3 3.3 0 0 1 3.3-3.3z",
        mark = "",
        stroke = 0f,
        fills = "M6.4 9.7h11.2a1 1 0 0 1 1 1v2.6a1 1 0 0 1-1 1H6.4a1 1 0 0 1-1-1v-2.6a1 1 0 0 1 1-1z",
    ),
}

internal fun Severity.look(): SeverityLook =
    when (this) {
        Severity.HINDRANCE -> SeverityLook.HINDRANCE
        Severity.ONLY_WITH_HELP -> SeverityLook.ONLY_WITH_HELP
        Severity.BLOCKING -> SeverityLook.BLOCKING
    }

/** Ścieżki znaku [look] gotowe do rysowania; parsowane raz na znak. */
internal class SeverityPaths(look: SeverityLook) {
    val shape: Path = PathParser().parsePathString(look.shape).toPath()
    val mark: Path = PathParser().parsePathString(look.mark).toPath()
    val fills: Path = PathParser().parsePathString(look.fills).toPath()
    val stroke: Stroke = Stroke(width = look.stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
}

/** Kreski i wypełnienia znaku [paths] w barwie [color], na siatce 24 × 24. */
internal fun DrawScope.drawSeverityMark(paths: SeverityPaths, color: Color) {
    drawPath(path = paths.mark, color = color, style = paths.stroke)
    drawPath(path = paths.fills, color = color)
}

/** Znak uciążliwości na cały krótszy bok [DrawScope]: kształt w [shapeColor], kreski i wypełnienia w [markColor]. */
internal fun DrawScope.drawSeverity(paths: SeverityPaths, shapeColor: Color, markColor: Color) {
    scale(scale = size.minDimension / SIGN_VIEWPORT, pivot = Offset.Zero) {
        drawPath(path = paths.shape, color = shapeColor)
        drawSeverityMark(paths = paths, color = markColor)
    }
}

/**
 * Znak uciążliwości [severity] jako obraz [sizePx] × [sizePx] dla mapy i jej legendy, z tych samych kształtów co
 * [SeverityIcon], w barwach znaku uciążliwości z [colors] (`znak_*`, `znak_*_symbol`), w konturze i obwódce.
 *
 * Wymagania: US1.3, NFR-21, NFR-16
 */
public fun mapSeverityImage(severity: Severity, sizePx: Int, colors: MapColors): Bitmap {
    val paths = SeverityPaths(severity.look())
    val (fill, symbol) = severity.mapTones(colors)
    return mapSignBitmap(sizePx = sizePx, shape = paths.shape, fill = fill, colors = colors) {
        drawSeverityMark(paths = paths, color = symbol)
    }
}

private fun Severity.mapTones(colors: MapColors): Pair<Color, Color> =
    when (this) {
        Severity.HINDRANCE -> colors.znakUtrudnienie to colors.znakUtrudnienieSymbol
        Severity.ONLY_WITH_HELP -> colors.znakTylkoZPomoca to colors.znakTylkoZPomocaSymbol
        Severity.BLOCKING -> colors.znakBlokada to colors.znakBlokadaSymbol
    }

/**
 * Znak uciążliwości obok napisu: trójkąt ze znakiem „!” dla „Utrudnienie”, tarcza z dwiema osobami dla „Tylko
 * z pomocą”, zaokrąglona tablica z jedną poziomą belką dla „Blokada”. Znaczenie niesie kształt, nie barwa.
 *
 * Kształt ma kolor `onSurface`, znak w nim `surface`; rozmiar
 * [io.github.dostepnykampus.core.designsystem.Ikony.rozmiar] skalowany jak tekst w sp. Ikona jest ozdobna:
 * TalkBack czyta nazwę uciążliwości z napisu obok.
 *
 * Wymagania: NFR-21, NFR-20
 *
 * @param severity uciążliwość, która wyznacza kształt
 * @param modifier modyfikator zewnętrzny
 */
@Composable
public fun SeverityIcon(severity: Severity, modifier: Modifier = Modifier) {
    val paths = remember(severity) { SeverityPaths(severity.look()) }
    val shapeColor = MaterialTheme.colorScheme.onSurface
    val markColor = MaterialTheme.colorScheme.surface
    Canvas(modifier = modifier.size(iconSizeLikeText())) {
        drawSeverity(paths = paths, shapeColor = shapeColor, markColor = markColor)
    }
}
