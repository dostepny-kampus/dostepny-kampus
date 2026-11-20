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
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.feature.map.R
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
