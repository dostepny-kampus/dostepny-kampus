/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.building.ResultCard
import io.github.dostepnykampus.feature.map.entrance.floorWord

private const val OTHER_FUNCTION = "Inne"
private const val SEPARATOR = " · "
private const val NO_BREAK_SPACE = '\u00A0'

/**
 * Wynik sali [found] pod polem szukaj: znak budynku, „Sala 329”, a pod numerem rodzaj pomieszczenia z danych PWr
 * i piętro, np. „Sala wykładowa · 2 piętro”; przy rodzaju „Inne” samo piętro. Cała karta to jeden cel dotyku
 * [onClick], a TalkBack czyta „Sala 329, budynek A-1, sala wykładowa, 2 piętro”.
 *
 * Wymagania: US18.1, NFR-17, NFR-18
 */
@Composable
internal fun RoomChoice(found: FoundRoom, onClick: () -> Unit) {
    val number = found.room.number.orEmpty()
    val kind = found.visibleKind()
    val floor = found.floor.label
    val title = stringResource(R.string.mapa_sala, number)
    val building = stringResource(R.string.mapa_budynek, found.building).replaceFirstChar(Char::lowercaseChar)
    val spoken = listOfNotNull(title, building, kind?.floorWord(), floor.floorWord())
    ResultCard(
        symbol = found.building,
        description = spoken.joinToString(separator = ", "),
        action = null,
        onClick = onClick,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = Typografia.wyroznienieWaga)
        Text(
            text = found.roomDetail(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Prośba pod polem szukaj, gdy tekst to sam numer sali bez budynku: „Dopisz budynek do numeru sali. Na przykład:
 * A-1 329.” TalkBack czyta ją sam, gdy się pojawi.
 *
 * Wymagania: US18.1, NFR-50
 */
@Composable
internal fun BuildingMissing() {
    Surface(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(Zaokraglenia.duze),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Text(
            text = stringResource(R.string.mapa_dopisz_budynek),
            modifier = Modifier.padding(Odstepy.m),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** Rodzaj pomieszczenia i piętro dla wyniku i karty sali. Wymagania: US18.1 */
internal fun FoundRoom.roomDetail(): String {
    val kind = visibleKind()
    val shownFloor = floor.label.replace(oldChar = ' ', newChar = NO_BREAK_SPACE)
    return listOfNotNull(kind, if (kind == null) shownFloor else shownFloor.floorWord()).joinToString(separator = SEPARATOR)
}

private fun FoundRoom.visibleKind(): String? = room.function.takeUnless { it.isBlank() || it == OTHER_FUNCTION }
