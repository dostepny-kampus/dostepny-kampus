/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.card.OutlinedAction
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.MapControlIcons

/**
 * Tryb po kolei karty [room]: „Krok N z M” z powrotem do listy, pasek postępu, sam bieżący krok drogi do sali
 * [roomTitle] z odsyłaczem szczegółów [onDetails] oraz „Wstecz” i „Dalej”, wyłączone na brzegach drogi.
 *
 * Wymagania: US18.2, NFR-17, NFR-18, NFR-21
 */
@Composable
internal fun RoomStepByStep(room: OpenRoom, roomTitle: String, onDetails: ((RoomStepDetail) -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        val card = room.card
        val index = card.step ?: 0
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.mapa_sala_krok_z, index + 1, card.stepCount),
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = { room.onStepByStep(false) }, modifier = Modifier.heightIn(min = CeleDotyku.minimum)) {
                Text(stringResource(R.string.mapa_sala_wszystkie_kroki))
            }
        }
        StepProgress(done = index + 1, count = card.stepCount)
        RoomWayStep(card = card, roomTitle = roomTitle, onDetails = onDetails)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CeleDotyku.odstep)) {
            val button = Modifier.weight(1f).heightIn(min = CeleDotyku.przycisk)
            OutlinedAction(onClick = { room.onStep(index - 1) }, modifier = button, enabled = card.hasPrevious) {
                Icon(imageVector = MapControlIcons.Left, contentDescription = null)
                Text(stringResource(R.string.mapa_sala_krok_wstecz))
            }
            Button(onClick = { room.onStep(index + 1) }, modifier = button, enabled = card.hasNext) {
                Text(stringResource(R.string.mapa_sala_krok_dalej))
                Icon(imageVector = MapControlIcons.Right, contentDescription = null)
            }
        }
    }
}

@Composable
private fun RoomWayStep(card: RoomCard, roomTitle: String, onDetails: ((RoomStepDetail) -> Unit)?) {
    val steps = (card.way as? RoomWay.Steps)?.wayRows(floor = card.floorText, roomTitle = roomTitle).orEmpty()
    val index = card.step ?: 0
    steps.getOrNull(index)?.let { step ->
        StepRow(number = index + 1, step = step, selected = true, isLink = false, modifier = Modifier.fillMaxWidth())
        val detail = step.detail
        if (detail != null && onDetails != null) {
            DetailLink(detail = detail, onClick = onDetails)
        }
    }
}

@Composable
private fun StepProgress(done: Int, count: Int) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Odstepy.xs)
                .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
    ) {
        repeat(count) { step ->
            val color = if (step < done) colors.primary else colors.surfaceVariant
            Box(
                Modifier
                    .weight(1f)
                    .height(PROGRESS_HEIGHT)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

private val PROGRESS_HEIGHT = 6.dp
