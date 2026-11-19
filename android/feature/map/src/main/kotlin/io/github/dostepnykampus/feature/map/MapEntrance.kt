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
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.SelectedEntrance
import io.github.dostepnykampus.feature.map.campus.selectedEntrance
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.overlay.spokenEntranceTitle

/**
 * Wejście [entrance] z [catalog] jako zaznaczenie na mapie; `null` bez katalogu, bez wejścia albo gdy katalog go
 * nie zna. Wejście bez nazwy dostaje podpis zastępczy.
 *
 * Wymagania: US3.3
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

/**
 * Wejście z pinezką i podpisem: wejście z otwartym arkuszem z [catalog], a gdy to wejście z trasy, to [chosen].
 * `null` bez otwartego arkusza wejścia, więc zamknięcie arkusza usuwa pinezkę.
 *
 * Wymagania: US3.3, NFR-53
 */
@Composable
internal fun SignSheetState.rememberPinnedEntrance(
    catalog: BuildingCatalog?,
    chosen: SelectedEntrance?,
): SelectedEntrance? {
    val unnamed = stringResource(R.string.karta_bez_nazwy)
    val openId = entranceId
    return remember(catalog, openId, chosen, unnamed) {
        when {
            openId == null -> null
            chosen?.id == openId -> chosen
            else -> catalog?.pinned(id = openId, unnamed = unnamed)
        }
    }
}

private fun BuildingCatalog.pinned(id: String, unnamed: String): SelectedEntrance? =
    entrances.firstOrNull { it.id == id }?.let { found ->
        selectedEntrance(catalog = this, id = id, buildingSymbol = found.buildingSymbol, unnamed = unnamed)
    }

/** Dane katalogu [data] razem z bieżącym zaznaczeniem [selection]: mapa jest, gdy są oba. */
internal data class MapScene(val data: CampusMapData, val selection: MapSelection)

/**
 * Scena mapy z danych [layers], zaznaczenia [focus] z trasy i nazwy terenu [area] wybranego przez osobę;
 * wejście bierze nazwę i punkt z [catalog]. `null`, dopóki nie ma danych mapy. Zmiana zaznaczenia nie kopiuje
 * danych katalogu.
 *
 * Wymagania: US1.7
 */
@Composable
internal fun rememberMapScene(
    layers: CampusMapData?,
    catalog: BuildingCatalog?,
    focus: MapFocus?,
    area: String?,
): MapScene? {
    val building = focus as? MapFocus.Building
    val chosen = rememberChosenEntrance(catalog, focus as? MapFocus.Entrance)
    return remember(layers, building?.symbol, building?.floor != null, chosen, area) {
        layers?.let { MapScene(it, it.focusSelection(building, chosen, area)) }
    }
}

/**
 * Zaznaczenie z polecenia trasy [focus], wejścia [chosen] i terenu [area]. Budynek z „Pokaż na planie” (z piętrem)
 * nie jest zaznaczony, bo karta jest już zamknięta, ale jego teren dalej wyznacza granicę mapy.
 *
 * Wymagania: US16.1, US2.9
 */
internal fun CampusMapData.focusSelection(focus: MapFocus?, chosen: SelectedEntrance?, area: String?): MapSelection {
    val building = focus as? MapFocus.Building
    val shown = selection(building?.symbol, chosen, area)
    return if (building?.floor != null) shown.copy(building = null) else shown
}

/**
 * Węzeł dla TalkBack: natywna mapa nie opisuje pinezki, więc czytany jest tekst „Zaznaczone wejście: »nazwa«,
 * budynek X” dla [chosen], z budynkiem ekranu; cudzysłów tylko przy nazwie z danych.
 *
 * Wymagania: NFR-18
 */
@Composable
internal fun EntranceDescription(chosen: SelectedEntrance) {
    val label = spokenEntranceTitle(chosen.label, isFromData = chosen.isLabelFromData)
    val text = stringResource(R.string.mapa_zaznaczone_wejscie, label, chosen.buildingSymbol)
    Spacer(Modifier.fillMaxSize().semantics { contentDescription = text })
}
