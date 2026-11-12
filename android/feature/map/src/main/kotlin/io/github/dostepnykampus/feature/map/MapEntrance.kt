/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.MapSelection
import io.github.dostepnykampus.feature.map.data.SelectedEntrance
import io.github.dostepnykampus.feature.map.data.selectedEntrance
import io.github.dostepnykampus.feature.map.data.selection
import io.github.dostepnykampus.feature.map.navigation.MapFocus

/**
 * Wejście [entrance] z [catalog] jako zaznaczenie na mapie; `null` bez katalogu, bez wejścia albo gdy katalog go
 * nie zna. Wejście bez nazwy dostaje podpis „Wejście bez nazwy w danych” (US3.3, decyzja 134).
 */
@Composable
internal fun rememberChosenEntrance(catalog: BuildingCatalog?, entrance: MapFocus.Entrance?): SelectedEntrance? {
    val unnamed = stringResource(R.string.karta_bez_nazwy)
    return remember(catalog, entrance, unnamed) {
        if (catalog == null || entrance == null) {
            null
        } else {
            selectedEntrance(
                catalog = catalog,
                id = entrance.id,
                buildingSymbol = entrance.buildingSymbol,
                unnamed = unnamed,
            )
        }
    }
}

/** Dane katalogu [data] razem z bieżącym zaznaczeniem [selection]: mapa jest, gdy są oba. */
internal data class MapScene(val data: CampusMapData, val selection: MapSelection)

/**
 * Scena mapy z danych [layers], zaznaczenia [focus] z trasy i nazwy terenu [area] wybranego przez osobę (US1.7);
 * wejście bierze nazwę i punkt z [catalog]. `null`, dopóki nie ma danych mapy. Zmiana zaznaczenia nie kopiuje
 * danych katalogu.
 */
@Composable
internal fun rememberMapScene(
    layers: CampusMapData?,
    catalog: BuildingCatalog?,
    focus: MapFocus?,
    area: String?,
): MapScene? {
    val building = (focus as? MapFocus.Building)?.symbol
    val chosen = rememberChosenEntrance(catalog, focus as? MapFocus.Entrance)
    return remember(layers, building, chosen, area) {
        layers?.let { MapScene(it, it.selection(building, chosen, area)) }
    }
}

/**
 * Węzeł dla TalkBack: natywna mapa nie opisuje pinezki, więc czytany jest tekst „Zaznaczone wejście: »nazwa«,
 * budynek X” dla [chosen], z budynkiem ekranu (NFR-18, decyzja 135).
 */
@Composable
internal fun EntranceDescription(chosen: SelectedEntrance) {
    val text = stringResource(R.string.mapa_zaznaczone_wejscie, chosen.label, chosen.buildingSymbol)
    Spacer(Modifier.fillMaxSize().semantics { contentDescription = text })
}
