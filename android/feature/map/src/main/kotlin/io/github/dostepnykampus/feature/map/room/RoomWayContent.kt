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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.GroupFrame
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Obramowania
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
            val steps = way.wayRows(floor = card.floorText, roomTitle = roomTitle)
            val links = onDetails.takeIf { onStep != null }
            GroupFrame {
                steps.forEachIndexed { index, step ->
                    if (index > 0) {
                        val isNextToChosen = card.step == index || card.step == index - 1
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = Odstepy.xs),
                            color = if (isNextToChosen) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    RoomWayRow(
                        number = index + 1,
                        step = step,
                        selected = card.step == index,
                        onClick = rowTap(index = index, detail = step.detail, onStep = onStep, onDetails = onDetails),
                        onDetails = links,
                    )
                }
            }
        }
    }
}

/** Piętro sali z twardą spacją przed „piętro”, żeby numer nie został sam na końcu linii. */
internal val RoomCard.floorText: String
    get() = found.floor.label.replace(oldValue = " piętro", newValue = "\u00A0piętro")

/** Krok drogi w wierszu: nazwa [title], zdanie [text] i szczegóły [detail]; sala nie ma szczegółów. */
internal data class WayRow(val title: String, val text: String, val detail: RoomStepDetail?)

/** Wiersze kroków drogi do sali [roomTitle] na piętrze [floor]: wejście, winda, jeśli jest, i sala. */
@Composable
internal fun RoomWay.Steps.wayRows(floor: String, roomTitle: String): List<WayRow> {
    val (entranceTitle, entranceText) = entranceLabels()
    val entrance = WayRow(title = entranceTitle, text = entranceText, detail = RoomStepDetail.ENTRANCE)
    val liftRow =
        lift?.let { step ->
            val text = stringResource(R.string.mapa_sala_droga_jedz, step.metersFromEntrance, floor.floorWord())
            WayRow(title = stringResource(R.string.karta_winda), text = text, detail = RoomStepDetail.LIFT)
        }
    val distance = if (lift == null) R.string.mapa_sala_droga_od_wejscia else R.string.mapa_sala_droga_od_windy
    val room = WayRow(title = roomTitle, text = stringResource(distance, floor, roomMeters), detail = null)
    return listOfNotNull(entrance, liftRow, room)
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
private fun RoomWayRow(
    number: Int,
    step: WayRow,
    selected: Boolean,
    onClick: (() -> Unit)?,
    onDetails: ((RoomStepDetail) -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Zaokraglenia.duze)
    val action = onClick?.let { Modifier.selectable(selected = selected, role = Role.Button, onClick = it) }
    val row =
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(action ?: Modifier)
            .padding(horizontal = Odstepy.s, vertical = Odstepy.xs)
    Surface(
        color = if (selected) colors.primaryContainer else Color.Transparent,
        shape = shape,
        border = BorderStroke(Obramowania.wybrane, colors.onPrimaryContainer).takeIf { selected },
    ) {
        Column {
            val isLink = onClick != null
            if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) {
                LargeStepRow(number = number, step = step, selected = selected, isLink = isLink, modifier = row)
            } else {
                StepRow(number = number, step = step, selected = selected, isLink = isLink, modifier = row)
            }
            val detail = step.detail
            if (selected && detail != null && onDetails != null) {
                DetailLink(detail = detail, onClick = onDetails)
            }
        }
    }
}

/** Krok [step] z numerem [number] w kółku, wypełnionym przy [selected], i „›” przy [isLink]. */
@Composable
internal fun StepRow(number: Int, step: WayRow, selected: Boolean, isLink: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
        verticalAlignment = Alignment.Top,
    ) {
        RoomStepBadge(number = number, size = 28.dp, selected = selected)
        Column(Modifier.weight(1f)) {
            Text(text = step.title, style = MaterialTheme.typography.titleMedium.copy(hyphens = Hyphens.Auto))
            Text(text = step.text, style = MaterialTheme.typography.bodyLarge)
        }
        if (isLink) {
            StepChevron()
        }
    }
}

@Composable
private fun LargeStepRow(
    number: Int,
    step: WayRow,
    selected: Boolean,
    isLink: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.s), verticalAlignment = Alignment.CenterVertically) {
            RoomStepBadge(number = number, size = 40.dp, selected = selected)
            Text(
                text = step.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(hyphens = Hyphens.Auto),
            )
            if (isLink) {
                StepChevron()
            }
        }
        Text(text = step.text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StepChevron() {
    Icon(imageVector = MapControlIcons.Right, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
}

@Composable
private fun RoomStepBadge(number: Int, size: Dp, selected: Boolean) {
    val map = DostepnyKampusTheme.mapColors
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        border = BorderStroke(Obramowania.ramka, if (selected) colors.primary else map.tusz),
        color = if (selected) colors.primary else map.papier,
        contentColor = if (selected) colors.onPrimary else map.tusz,
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
    detail: RoomStepDetail?,
    onStep: ((Int) -> Unit)?,
    onDetails: ((RoomStepDetail) -> Unit)?,
): (() -> Unit)? =
    when {
        onStep != null -> {
            { onStep(index) }
        }

        detail != null && onDetails != null -> {
            { onDetails(detail) }
        }

        else -> {
            null
        }
    }

/** Odsyłacz „Szczegóły …” kroku [detail] z celem dotyku 48 dp. */
@Composable
internal fun DetailLink(detail: RoomStepDetail, onClick: (RoomStepDetail) -> Unit) {
    TextButton(
        onClick = { onClick(detail) },
        modifier = Modifier.padding(start = Odstepy.s).heightIn(min = CeleDotyku.minimum),
    ) {
        Text(stringResource(detail.title))
        Icon(imageVector = MapControlIcons.Right, contentDescription = null)
    }
}
