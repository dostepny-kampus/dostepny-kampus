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
import io.github.dostepnykampus.feature.map.data.SelectedEntrance
import io.github.dostepnykampus.feature.map.data.onAreaOf
import io.github.dostepnykampus.feature.map.data.selectedEntrance
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

/**
 * Węzeł dla TalkBack: natywna mapa nie opisuje pinezki, więc czytany jest tekst „Zaznaczone wejście: »nazwa«,
 * budynek X” dla [chosen], z budynkiem ekranu (NFR-18, decyzja 135).
 */
@Composable
internal fun EntranceDescription(chosen: SelectedEntrance) {
    val text = stringResource(R.string.mapa_zaznaczone_wejscie, chosen.label, chosen.buildingSymbol)
    Spacer(Modifier.fillMaxSize().semantics { contentDescription = text })
}

/**
 * Dane mapy z zaznaczeniem [focus] i wejściem [chosen] na terenie pokazanego budynku: [shown], a bez niego [focus]
 * albo budynek wejścia. Budynek z innego terenu przenosi na swój teren granicę mapy (US1.8, BR-G5).
 */
@Composable
internal fun rememberMapData(
    layers: CampusMapData?,
    focus: String?,
    chosen: SelectedEntrance?,
    shown: String?,
): CampusMapData? {
    val symbol = shown ?: focus ?: chosen?.buildingSymbol
    return remember(layers, focus, chosen, symbol) {
        layers?.run { onAreaOf(symbol).copy(focus = focus, selectedEntrance = chosen) }
    }
}
