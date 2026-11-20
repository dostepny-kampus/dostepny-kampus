/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.entrance.floorWord
import io.github.dostepnykampus.feature.map.overlay.MapControlIcons

/** Wiersze drogi [card] i odsyłacze do szczegółów. Wymagania: US18.2, US18.3 */
@Composable
internal fun RoomWayContent(
    card: RoomCard,
    roomTitle: String,
    onStep: ((Int) -> Unit)?,
    onDetails: ((RoomStepDetail) -> Unit)?,
) {
    val label = card.found.floor.label
    val floor = label.replace(oldValue = " piętro", newValue = "\u00A0piętro")
    when (val way = card.way) {
        null -> {
            Text(stringResource(R.string.mapa_sala_droga_ladowanie))
        }

        RoomWay.NoEntrance -> {
            Text(stringResource(R.string.mapa_sala_droga_brak_wejscia))
        }

        RoomWay.NoLift -> {
            Text(stringResource(R.string.mapa_sala_droga_brak_windy))
        }

        is RoomWay.Steps -> {
            val (entranceTitle, entranceText) = way.entranceLabels()
            RoomWayRow(
                number = 1,
                title = entranceTitle,
                text = entranceText,
                selected = card.step == 0,
                onClick = rowTap(index = 0, detail = RoomStepDetail.ENTRANCE, onStep = onStep, onDetails = onDetails),
            )
            if (card.step == 0 && onStep != null && onDetails != null) {
                DetailLink(detail = RoomStepDetail.ENTRANCE, onClick = onDetails)
            }
            way.lift?.let { lift ->
                RoomWayRow(
                    number = 2,
                    title = stringResource(R.string.karta_winda),
                    text = stringResource(R.string.mapa_sala_droga_jedz, lift.metersFromEntrance, floor.floorWord()),
                    selected = card.step == 1,
                    onClick = rowTap(index = 1, detail = RoomStepDetail.LIFT, onStep = onStep, onDetails = onDetails),
                )
                if (card.step == 1 && onStep != null && onDetails != null) {
                    DetailLink(detail = RoomStepDetail.LIFT, onClick = onDetails)
                }
            }
            val roomDistance =
                if (way.lift == null) R.string.mapa_sala_droga_od_wejscia else R.string.mapa_sala_droga_od_windy
            val lastStep = card.stepCount - 1
            RoomWayRow(
                number = lastStep + 1,
                title = roomTitle,
                text = stringResource(roomDistance, floor, way.roomMeters),
                selected = card.step == lastStep,
                onClick = onStep?.let { { it(lastStep) } },
            )
        }
    }
}

@Composable
private fun RoomWay.Steps.entranceLabels(): Pair<String, String> {
    val title = entrance.name?.takeIf { entrance.hasName } ?: stringResource(R.string.mapa_sala_droga_wejscie)
    val label = entranceFloor.label
    val floor = if (isFloorAssumed) stringResource(R.string.mapa_sala_droga_parter, label) else label
    val place = entrance.placeDescription.ifBlank { entrance.approach }
    return title to listOf(floor, place).filter(String::isNotBlank).joinToString(separator = ". ")
}

@Composable
private fun RoomWayRow(number: Int, title: String, text: String, selected: Boolean, onClick: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val isLarge = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val shape = RoundedCornerShape(Zaokraglenia.duze)
    val action = onClick?.let { Modifier.selectable(selected = selected, role = Role.Button, onClick = it) }
    val row = Modifier.fillMaxWidth().clip(shape).then(action ?: Modifier)
    Surface(color = if (selected) colors.primaryContainer else colors.surface, shape = shape) {
        if (isLarge) {
            Column(row.padding(vertical = Odstepy.s), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
                HorizontalDivider(color = colors.outlineVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoomStepBadge(number = number, size = 40.dp, selected = selected)
                    Text(text = title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    if (onClick != null) {
                        Icon(imageVector = MapControlIcons.Right, contentDescription = null, tint = colors.primary)
                    }
                }
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Row(
                modifier = row.padding(Odstepy.s),
                horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
                verticalAlignment = Alignment.Top,
            ) {
                RoomStepBadge(number = number, size = 28.dp, selected = selected)
                Column(Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall)
                    Text(text = text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun RoomStepBadge(number: Int, size: Dp, selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        border = BorderStroke(1.dp, colors.outline),
        color = if (selected) colors.primary else colors.surface,
    ) {
        Box(
            modifier = Modifier.sizeIn(minWidth = size, minHeight = size).padding(horizontal = Odstepy.xs),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = number.toString(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun rowTap(
    index: Int,
    detail: RoomStepDetail,
    onStep: ((Int) -> Unit)?,
    onDetails: ((RoomStepDetail) -> Unit)?,
): (() -> Unit)? = onStep?.let { choose -> { choose(index) } } ?: onDetails?.let { open -> { open(detail) } }

@Composable
private fun DetailLink(detail: RoomStepDetail, onClick: (RoomStepDetail) -> Unit) {
    TextButton(onClick = { onClick(detail) }) { Text(stringResource(detail.title)) }
}
