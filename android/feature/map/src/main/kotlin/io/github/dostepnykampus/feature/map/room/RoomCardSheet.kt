/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.BuildingLink
import io.github.dostepnykampus.feature.map.overlay.MapSheet
import io.github.dostepnykampus.feature.map.overlay.MapSheetHeader

/**
 * Karta [room] z drogą tekstem. [onShowMap] oznacza pełny ekran z powrotem do planu.
 *
 * Wymagania: US18.1, US18.2, US18.3
 */
@Composable
internal fun RoomCardSheet(
    room: OpenRoom,
    onBuilding: (String) -> Unit,
    modifier: Modifier = Modifier,
    onShowMap: (() -> Unit)? = null,
) {
    val (card, onClose) = room
    val title = stringResource(R.string.mapa_sala, found.room.number.orEmpty())
    MapSheet(modifier = modifier, isFull = onShowMap != null) {
        if (onShowMap == null) {
            MapSheetHeader(title = title, onClose = onClose)
        } else {
            BackHeader(title = title, onBack = onClose, horizontalPadding = 0.dp)
        }
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
                text = stringResource(R.string.mapa_sala_pokaz_na_mapie),
                onClick = onShowMap,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
