/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.model.EntranceMarking

private const val ICON_VIEWPORT = 24f

private const val MARK_STROKE = 2.6f

private val MinHeight = 36.dp

private enum class MarkingLook(
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

/**
 * Oznaczenie wejścia: kształt ikony, słowo ze słownika użytkownika i spokojny kontener (NFR-21, decyzja
 * projektu nr 48 pkt 3). Znaczenie niesie kształt i słowo, nie barwa: koło ze znakiem ✓ i „Dostępne”,
 * ośmiokąt ze znakiem ✕ i „Niedostępne”, romb ze znakiem ? i „Brak danych”.
 *
 * Tokeny: kontener `status…Container`, kształt i słowo `onStatus…Container`, ramka [Obramowania.ramka] w
 * kolorze `outline`, zaokrąglenie [Zaokraglenia.standard], napis `labelLarge` z [Typografia.wyroznienieWaga],
 * ikona [Ikony.rozmiar] skalowana jak tekst w sp (NFR-20). Wysokość co najmniej 36 dp jak w makietach V2 i V3;
 * oznaczenie nie jest celem dotyku. Ikona jest ozdobna: TalkBack czyta słowo.
 */
@Composable
fun MarkingChip(marking: EntranceMarking, modifier: Modifier = Modifier) {
    val look =
        when (marking) {
            EntranceMarking.ACCESSIBLE -> MarkingLook.ACCESSIBLE
            EntranceMarking.INACCESSIBLE -> MarkingLook.INACCESSIBLE
            EntranceMarking.NO_DATA -> MarkingLook.NO_DATA
        }
    val container = look.container(DostepnyKampusTheme.statusColors)
    val content = look.content(DostepnyKampusTheme.statusColors)
    Surface(
        modifier = modifier.border(Obramowania.ramka, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = MinHeight)
                    .padding(start = Odstepy.s, end = Odstepy.m, top = Odstepy.xs, bottom = Odstepy.xs),
            horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MarkingIcon(look = look, shapeColor = content, markColor = container)
            Text(
                text = stringResource(look.word),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = Typografia.wyroznienieWaga,
            )
        }
    }
}

@Composable
private fun MarkingIcon(look: MarkingLook, shapeColor: Color, markColor: Color) {
    val shape = remember(look) { PathParser().parsePathString(look.shape).toPath() }
    val mark = remember(look) { PathParser().parsePathString(look.mark).toPath() }
    Canvas(modifier = Modifier.size(iconSizeLikeText())) {
        scale(scale = size.minDimension / ICON_VIEWPORT, pivot = Offset.Zero) {
            drawPath(path = shape, color = shapeColor)
            drawPath(
                path = mark,
                color = markColor,
                style = Stroke(width = MARK_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
