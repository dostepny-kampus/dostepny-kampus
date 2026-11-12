/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.feature.map.data.MapArea

/**
 * Lista budynków V1 podzielona nagłówkami terenów z liczbą budynków, np. „Wittigowo · 9” (US1.8, NFR-19). W terenie
 * budynki stoją w kolejności katalogu, a wpisany symbol [query] zawęża listę; teren bez pasującego budynku nie ma
 * nagłówka. Nagłówek jest nagłówkiem dla TalkBack, który czyta „Wittigowo, 9 budynków” (NFR-18). Bez danych mapy
 * lista nie ma nagłówków.
 *
 * @param catalog katalog budynków; `null`, dopóki się wczytuje
 * @param query tekst z pola symbolu
 * @param search wynik wyszukiwania z modelu widoku; lista pokazuje go tylko dla [query] (NFR-11, #1053)
 * @param areas tereny w kolejności nagłówków
 * @param onBuildingClick symbol budynku wybranego na liście
 * @param modifier wymiary listy
 */
@Composable
internal fun BuildingList(
    catalog: BuildingCatalog?,
    query: String,
    search: BuildingSearch,
    areas: AreaOrder,
    onBuildingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buildings = if (query.isBlank()) catalog?.buildings.orEmpty() else search.foundFor(query).orEmpty()
    val groups =
        remember(buildings, areas) {
            areas.areas.associateWith { area -> buildings.filter { it.symbol in area.symbols } }
        }
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    LazyColumn(
        modifier = modifier.fillMaxWidth().windowInsetsPadding(insets),
        contentPadding = PaddingValues(Odstepy.m),
        verticalArrangement = Arrangement.spacedBy(Odstepy.s),
    ) {
        items(buildings - groups.values.flatten().toSet(), key = Building::symbol) { building ->
            BuildingChoice(building) { onBuildingClick(building.symbol) }
        }
        groups.filterValues { it.isNotEmpty() }.forEach { (area, inArea) ->
            item(key = AREA_KEY + area.name) { AreaHeading(area) }
            items(inArea, key = Building::symbol) { building ->
                BuildingChoice(building) { onBuildingClick(building.symbol) }
            }
        }
    }
}

private const val AREA_KEY = "teren:"

@Composable
private fun AreaHeading(area: MapArea, modifier: Modifier = Modifier) {
    val count = area.symbols.size
    val buildings = stringResource(buildingsWord(count), count)
    val spoken = stringResource(R.string.mapa_teren_naglowek_opis, area.name, buildings)
    Text(
        text = stringResource(R.string.mapa_teren_naglowek, area.name, count),
        modifier =
            modifier.padding(top = Odstepy.s).semantics {
                heading()
                contentDescription = spoken
            },
        style = MaterialTheme.typography.titleMedium,
    )
}
