/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.area.AreaOrder
import io.github.dostepnykampus.feature.map.area.otherAreaOf
import io.github.dostepnykampus.feature.map.building.BuildingChoice
import io.github.dostepnykampus.feature.map.building.BuildingSearch
import io.github.dostepnykampus.feature.map.building.MissingRoom
import io.github.dostepnykampus.feature.map.building.foundFor
import io.github.dostepnykampus.feature.map.room.BuildingMissing
import io.github.dostepnykampus.feature.map.room.RoomChoice
import io.github.dostepnykampus.feature.map.room.RoomMissing

/**
 * Wyniki pod polem szukaj V1 dla tekstu [query]: budynki albo sale, przy samym numerze sali prośba o budynek,
 * a bez wyniku karta „brak wyniku”. Wyniki budynków i sal są tylko przy mapie; prośba i „brak wyniku” także przy
 * liście. Bez niczego do pokazania nie ma panelu.
 *
 * Wymagania: US1.1, US18.1, NFR-18, NFR-50
 *
 * @param search wynik z modelu widoku; liczy się tylko dla zapytania [query]
 * @param query tekst z pola
 * @param isList czy pod polem jest lista budynków
 * @param isHidden czy wyniki są schowane po wyborze albo po utracie fokusu pola
 * @param areas tereny do dopisku o innym terenie przy budynku
 * @param onBuildingClick symbol wybranego budynku
 * @param onRoomClick wybrana sala, po [onRelease]
 * @param onRelease chowa wyniki i klawiaturę przed wyborem sali
 * @param onShowList „Pokaż listę” z karty „brak wyniku” przy mapie
 */
@Composable
internal fun SearchResults(
    search: BuildingSearch,
    query: String,
    isList: Boolean,
    isHidden: Boolean,
    areas: AreaOrder,
    onBuildingClick: (String) -> Unit,
    onRoomClick: (FoundRoom) -> Unit,
    onRelease: () -> Unit,
    onShowList: () -> Unit,
) {
    val results = shownResults(search = search, query = query, isList = isList, isHidden = isHidden)
    val hasItems = results.buildings.isNotEmpty() || results.rooms.isNotEmpty() || results.missingRoom != null
    if (!hasItems && results.note == null) return
    ResultPanel {
        results.missingRoom?.let { RoomMissing(it) }
        results.buildings.forEach { building ->
            BuildingChoice(building, areas.otherAreaOf(building.symbol)) { onBuildingClick(building.symbol) }
        }
        results.rooms.forEach { room ->
            RoomChoice(room) {
                onRelease()
                onRoomClick(room)
            }
        }
        when (results.note) {
            ResultNote.BUILDING_MISSING -> BuildingMissing()
            ResultNote.NO_RESULT -> NoResult(query, onShowList = onShowList.takeUnless { isList })
            null -> Unit
        }
    }
}

/** Zdanie pod wynikami pola szukaj. Wymagania: US1.1, US18.1 */
internal enum class ResultNote {
    /** Sam numer sali bez budynku: prośba o budynek. */
    BUILDING_MISSING,

    /** Nic nie pasuje: karta „brak wyniku”. */
    NO_RESULT,
}

/**
 * Wyniki do pokazania pod polem.
 *
 * Wymagania: US1.1, US18.1
 *
 * @property buildings budynki, tylko przy mapie z widocznymi wynikami
 * @property rooms sale, tylko przy mapie z widocznymi wynikami
 * @property note zdanie pod wynikami, przy mapie z widocznymi wynikami i przy liście; `null` bez zdania
 * @property missingRoom zdanie o sali, której nie ma w danych budynku; przy mapie wiersz budynku jest w [buildings]
 */
internal data class ShownResults(
    val buildings: List<Building>,
    val rooms: List<FoundRoom>,
    val note: ResultNote?,
    val missingRoom: MissingRoom?,
)

/**
 * Wyniki z [search] dla tekstu [query] z pola: wynik liczy się tylko dla tego samego zapytania. Przy liście
 * ([isList]) albo po schowaniu ([isHidden]) budynków i sal nie ma, a zdanie zostaje przy liście.
 *
 * Wymagania: US1.1, US18.1, US18.3
 */
internal fun shownResults(search: BuildingSearch, query: String, isList: Boolean, isHidden: Boolean): ShownResults {
    val isCurrent = search.query == query
    val found = search.foundFor(query)
    val rooms = search.rooms.takeIf { isCurrent }.orEmpty()
    val missing = search.missingRoom.takeIf { isCurrent }
    val isEmpty = query.isNotBlank() && found?.isEmpty() == true && rooms.isEmpty() && missing == null
    val note = resultNote(isMissing = isCurrent && search.isBuildingMissing, isEmpty = isEmpty)
    val isOnMap = !isList && !isHidden
    return ShownResults(
        buildings = (found.orEmpty() + listOfNotNull(missing?.building)).takeIf { isOnMap }.orEmpty(),
        rooms = rooms.takeIf { isOnMap }.orEmpty(),
        note = note.takeIf { isList || !isHidden },
        missingRoom = missing.takeIf { isList || !isHidden },
    )
}

private fun resultNote(isMissing: Boolean, isEmpty: Boolean): ResultNote? =
    when {
        isMissing -> ResultNote.BUILDING_MISSING
        isEmpty -> ResultNote.NO_RESULT
        else -> null
    }

@Composable
private fun ResultPanel(content: @Composable ColumnScope.() -> Unit) {
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.windowInsetsPadding(insets).padding(horizontal = Odstepy.m, vertical = Odstepy.s),
            verticalArrangement = Arrangement.spacedBy(Odstepy.s),
            content = content,
        )
    }
}

@Composable
private fun NoResult(query: String, onShowList: (() -> Unit)?) {
    Surface(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(Zaokraglenia.duze),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            Text(
                text = stringResource(R.string.mapa_brak_wyniku, query.trim()),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(text = stringResource(R.string.mapa_brak_wyniku_rada), style = MaterialTheme.typography.bodyLarge)
            onShowList?.let { showList ->
                val buttonModifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.przycisk)
                FilledTonalButton(onClick = showList, modifier = buttonModifier) {
                    Text(text = stringResource(R.string.mapa_pokaz_liste), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
