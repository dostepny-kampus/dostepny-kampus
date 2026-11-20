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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.entrance.floorWord

/** Wiersze drogi do sali. Wymagania: US18.2, US18.3 */
@Composable
internal fun RoomWayContent(way: RoomWay?, floor: String, roomTitle: String) {
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
    val isLarge = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val badgeSize = if (isLarge) 48.dp else 28.dp
    Row(
        horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
        verticalAlignment = if (isLarge) Alignment.CenterVertically else Alignment.Top,
    ) {
        Surface(
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Box(
                modifier = Modifier.sizeIn(minWidth = badgeSize, minHeight = badgeSize).padding(horizontal = Odstepy.xs),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = number.toString(), style = MaterialTheme.typography.labelLarge)
            }
        }
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
