/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.model.EntranceMarking

private val MinHeight = 36.dp

/**
 * Oznaczenie wejścia: kształt ikony, słowo ze słownika użytkownika i spokojny kontener bez ramki, bo nie jest
 * przyciskiem. Znaczenie niesie kształt i słowo, nie barwa: koło z ✓ i „Dostępne”, ośmiokąt z ✕ i „Niedostępne”, romb z
 * ? i „Brak danych”. Wysokość co najmniej 36 dp; oznaczenie nie jest celem dotyku, a TalkBack czyta tylko słowo.
 *
 * Wymagania: NFR-21, NFR-20
 *
 * @param marking oznaczenie, które wyznacza kształt ikony i barwy
 * @param modifier modyfikator zewnętrzny
 * @param label własne słowo obok ikony; `null` daje słowo ze słownika oznaczenia
 */
@Composable
public fun MarkingChip(marking: EntranceMarking, modifier: Modifier = Modifier, label: String? = null) {
    val look = marking.look()
    val container = look.container(DostepnyKampusTheme.statusColors)
    val content = look.content(DostepnyKampusTheme.statusColors)
    Surface(
        modifier = modifier,
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
                text = label ?: stringResource(look.word),
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
        drawMarking(shape = shape, mark = mark, shapeColor = shapeColor, markColor = markColor)
    }
}
