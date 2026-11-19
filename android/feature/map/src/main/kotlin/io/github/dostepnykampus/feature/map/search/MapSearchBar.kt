/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.feature.map.CampusMapUiState
import io.github.dostepnykampus.feature.map.CampusSearch
import io.github.dostepnykampus.feature.map.area.AreaOrder
import io.github.dostepnykampus.feature.map.slideUp

/**
 * Góra V1 ze stanem ekranu [state]: [CampusSearch] nad mapą, a przy liście ([isList]) na całą wysokość. Pasek
 * odjeżdża w górę o część [barExit] swojej wysokości.
 *
 * Wymagania: US1.1, US18.1
 *
 * @param state katalog, wynik wyszukiwania, internet i chwila pobrania danych
 * @param isList czy pod górą jest lista budynków
 * @param areas tereny w kolejności nagłówków listy
 * @param onSearch zapytanie z pola do policzenia w modelu widoku
 * @param onListChange wybór widoku: prawda dla listy, fałsz dla mapy
 * @param onBuildingClick wybrany budynek z wyników albo z listy
 * @param onRoomClick wybrana sala z wyników
 * @param barExit część wysokości paska, o którą odjechał w górę
 * @param onBarHeight wysokość całej góry w pikselach, z wynikami
 * @param onHeaderHeight wysokość góry bez wyników w pikselach
 */
@Composable
internal fun MapSearchBar(
    state: CampusMapUiState,
    isList: Boolean,
    areas: AreaOrder,
    onSearch: (String) -> Unit,
    onListChange: (Boolean) -> Unit,
    onBuildingClick: (String) -> Unit,
    onRoomClick: (FoundRoom) -> Unit,
    barExit: Animatable<Float, AnimationVector1D>,
    onBarHeight: (Int) -> Unit,
    onHeaderHeight: (Int) -> Unit,
) {
    CampusSearch(
        catalog = state.catalog,
        search = state.search,
        onSearch = onSearch,
        isOnline = state.isOnline,
        syncedAt = state.syncedAt,
        isList = isList,
        areas = areas,
        onListChange = onListChange,
        onBuildingClick = onBuildingClick,
        onRoomClick = onRoomClick,
        onBarHeight = onHeaderHeight,
        modifier =
            (if (isList) Modifier.fillMaxSize() else Modifier)
                .slideUp(barExit)
                .onSizeChanged { onBarHeight(it.height) },
    )
}
