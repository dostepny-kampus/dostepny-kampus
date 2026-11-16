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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import io.github.dostepnykampus.core.designsystem.iconSizeLikeText
import io.github.dostepnykampus.core.model.submission.Severity

private const val SIGN_VIEWPORT = 24f
private const val HALO = 1f

/**
 * Kształty znaku uciążliwości na siatce 24 × 24, jak w makiecie hi-fi V1.
 *
 * Wymagania: NFR-21
 *
 * @property shape wypełniony kształt: trójkąt, tarcza albo zaokrąglona kwadratowa tablica
 * @property mark kreski w kształcie: wykrzyknik albo ramiona osób; pusty napis, gdy ich brak
 * @property stroke grubość kresek; 0 bez kresek
 * @property fills wypełnione części w kolorze kresek: kropka wykrzyknika, głowy osób albo pozioma belka; pusty
 *   napis, gdy ich brak
 */
internal enum class SeverityLook(val shape: String, val mark: String, val stroke: Float, val fills: String) {
    HINDRANCE(
        shape = "M12 1.8L23 21.2H1z",
        mark = "M12 8.6v6",
        stroke = 2.4f,
        fills = "M12 16.2a1.4 1.4 0 1 0 0 2.8a1.4 1.4 0 1 0 0-2.8z",
    ),
    ONLY_WITH_HELP(
        shape = "M2.5 2.5h19v10L12 22.5 2.5 12.5z",
        mark = "M5.6 14.4c0-2.2 1.3-3.6 3-3.6s3 1.4 3 3.6M12.4 14.4c0-2.2 1.3-3.6 3-3.6s3 1.4 3 3.6",
        stroke = 2f,
        fills = "M8.6 5.6a2 2 0 1 0 0 4a2 2 0 1 0 0-4zM15.4 5.6a2 2 0 1 0 0 4a2 2 0 1 0 0-4z",
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

/**
 * Znak uciążliwości na cały krótszy bok [DrawScope]: kształt w [shapeColor], kreski i wypełnienia w [markColor].
 * Z [hasHalo] kształt dostaje obwódkę w [markColor], żeby odcinał się od podkładu mapy.
 */
internal fun DrawScope.drawSeverity(paths: SeverityPaths, shapeColor: Color, markColor: Color, hasHalo: Boolean) {
    val margin = if (hasHalo) HALO else 0f
    scale(scale = size.minDimension / (SIGN_VIEWPORT + 2 * margin), pivot = Offset.Zero) {
        translate(left = margin, top = margin) {
            if (hasHalo) {
                drawPath(path = paths.shape, color = markColor, style = Stroke(2 * margin, join = StrokeJoin.Round))
            }
            drawPath(path = paths.shape, color = shapeColor)
            drawPath(path = paths.mark, color = markColor, style = paths.stroke)
            drawPath(path = paths.fills, color = markColor)
        }
    }
}

/**
 * Znak uciążliwości [severity] jako obraz [sizePx] × [sizePx] dla warstwy mapy, z tych samych
 * kształtów co [SeverityIcon]: [shapeColor] to kształt, [markColor] znak w nim i obwódka wokół kształtu.
 *
 * Wymagania: US1.3, NFR-21
 */
public fun severityImage(severity: Severity, sizePx: Int, shapeColor: Color, markColor: Color): Bitmap {
    val paths = SeverityPaths(severity.look())
    return signBitmap(sizePx) {
        drawSeverity(paths = paths, shapeColor = shapeColor, markColor = markColor, hasHalo = true)
    }
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
        drawSeverity(paths = paths, shapeColor = shapeColor, markColor = markColor, hasHalo = false)
    }
}
