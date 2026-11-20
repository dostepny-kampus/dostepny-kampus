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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.entrance.floorWord
import io.github.dostepnykampus.feature.map.overlay.BuildingLink
import io.github.dostepnykampus.feature.map.overlay.MapSheet
import io.github.dostepnykampus.feature.map.overlay.MapSheetHeader

/** Otwarta karta [card], którą zamyka [onClose]. Wymagania: US18.1 */
data class OpenRoom(val card: RoomCard, val onClose: () -> Unit)

/** Karta [card] z drogą tekstem. [onShowMap] oznacza pełny ekran z powrotem do planu. Wymagania: US18.1, US18.2, US18.3 */
@Composable
internal fun RoomCardSheet(
    card: RoomCard,
    onClose: () -> Unit,
    onBuilding: (String) -> Unit,
    modifier: Modifier = Modifier,
    onShowMap: (() -> Unit)? = null,
) {
    val found = card.found
    val title = stringResource(R.string.mapa_sala, found.room.number.orEmpty())
    MapSheet(modifier = modifier, isFull = onShowMap != null) {
        if (onShowMap == null) MapSheetHeader(title = title, onClose = onClose) else BackHeader(title, onBack = onClose)
        Text(text = found.roomDetail(), style = MaterialTheme.typography.bodyLarge)
        BuildingLink(symbol = found.building, onClick = { onBuilding(found.building) })
        Text(
            text = stringResource(R.string.mapa_sala_droga),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        RoomWayContent(way = card.way, floor = found.floor.label, roomTitle = title)
        if (onShowMap != null) {
            PinnedButton(
                text = stringResource(R.string.mapa_sala_pokaz_mape),
                onClick = onShowMap,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RoomWayContent(way: RoomWay?, floor: String, roomTitle: String) {
    when (way) {
        null -> Text(stringResource(R.string.mapa_sala_droga_ladowanie))
        RoomWay.NoEntrance -> Text(stringResource(R.string.mapa_sala_droga_brak_wejscia))
        RoomWay.NoLift -> Text(stringResource(R.string.mapa_sala_droga_brak_windy))
        is RoomWay.Steps -> {
            val entranceTitle =
                way.entrance.name?.takeIf { way.entrance.hasName } ?: stringResource(R.string.mapa_sala_droga_wejscie)
            val entranceFloor =
                if (way.isFloorAssumed) {
                    stringResource(R.string.mapa_sala_droga_parter, way.entranceFloor.label)
                } else {
                    way.entranceFloor.label
                }
            val place = way.entrance.placeDescription.ifBlank { way.entrance.approach }
            val entranceText = listOf(entranceFloor, place).filter(String::isNotBlank).joinToString(separator = ". ")
            RoomWayRow(number = 1, title = entranceTitle, text = entranceText)
            way.lift?.let {
                RoomWayRow(
                    number = 2,
                    title = stringResource(R.string.mapa_sala_droga_winda),
                    text = stringResource(R.string.mapa_sala_droga_jedz, it.metersFromEntrance, floor.floorWord()),
                )
            }
            val roomDistance =
                if (way.lift == null) R.string.mapa_sala_droga_od_wejscia else R.string.mapa_sala_droga_od_windy
            RoomWayRow(
                number = if (way.lift == null) 2 else 3,
                title = roomTitle,
                text = stringResource(roomDistance, floor, way.roomMeters),
            )
        }
    }
}

@Composable
private fun RoomWayRow(number: Int, title: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.s), verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                Text(text = number.toString(), style = MaterialTheme.typography.labelLarge)
            }
        }
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
