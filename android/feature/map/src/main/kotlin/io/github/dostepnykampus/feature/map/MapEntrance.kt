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
import io.github.dostepnykampus.feature.map.data.SelectedEntrance
import io.github.dostepnykampus.feature.map.data.selectedEntrance
import io.github.dostepnykampus.feature.map.navigation.MapFocus

// Wejście [entrance] z katalogu jako zaznaczenie na mapie; podpis zastępczy, gdy dane nie mają nazwy (decyzja 134).
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

// Natywna mapa nie ma opisu pinezki, więc TalkBack czyta zaznaczone wejście i budynek ekranu z tego węzła (NFR-18).
@Composable
internal fun EntranceDescription(chosen: SelectedEntrance) {
    val text = stringResource(R.string.mapa_zaznaczone_wejscie, chosen.label, chosen.buildingSymbol)
    Spacer(Modifier.fillMaxSize().semantics { contentDescription = text })
}
