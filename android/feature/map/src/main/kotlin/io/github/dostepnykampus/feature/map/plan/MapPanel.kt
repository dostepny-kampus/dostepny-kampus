/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import io.github.dostepnykampus.feature.map.navigation.MapFocus

/**
 * Karta albo arkusz nad mapą V1: budynek, którego plan trzyma, i dół mapy, który zasłania. Plan bierze z niego
 * wybrany budynek, a kamera dopełnienie od dołu, więc środek mapy to środek części widocznej nad arkuszem.
 *
 * Wymagania: US16.1, US16.2, US2.8, US18.1, BR-G1
 *
 * @property building symbol budynku trzymającego plan; `null`, gdy wyborem planu rządzi środek mapy
 * @property cover wysokość zasłoniętego dołu mapy w pikselach; 0 bez dopełnienia kamery
 */
internal data class MapPanel(val building: String? = null, val cover: Int = 0)

/**
 * Arkusz nad mapą przy zaznaczeniu [focus]. Otwarty arkusz wejścia budynku [entranceBuilding] trzyma ten budynek
 * i zasłania dół mapy na [entranceCover] pikseli. Bez niego budynek trzyma zaznaczenie według [selectedBuilding];
 * zaznaczone wejście z zamkniętym arkuszem już nic nie trzyma.
 *
 * Wymagania: US16.1, US16.2, US2.8, US3.3, BR-G1
 *
 * @param focus zaznaczenie z trasy
 * @param entranceBuilding symbol budynku otwartego arkusza wejścia; `null` bez arkusza wejścia
 * @param entranceCover wysokość arkusza wejścia zasłaniającego dół mapy w pikselach; 0 bez dopełnienia
 */
internal fun mapPanel(focus: MapFocus?, entranceBuilding: String?, entranceCover: Int): MapPanel =
    MapPanel(
        building = entranceBuilding ?: selectedBuilding(focus).takeUnless { focus is MapFocus.Entrance },
        cover = entranceCover,
    )

/**
 * Symbol budynku trzymającego plan przy zaznaczeniu [focus] albo `null`, gdy wyborem rządzi środek mapy.
 * Karta V2, karta sali i arkusz wejścia trzymają swój budynek; „Pokaż na mapie” i „Pokaż na planie” zostawiają
 * wybór z mapy.
 */
internal fun selectedBuilding(focus: MapFocus?): String? =
    when (focus) {
        null -> null
        is MapFocus.Building -> focus.run { symbol.takeIf { if (floor == null) command == null else room != null } }
        is MapFocus.Entrance -> focus.buildingSymbol
    }
