/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import io.github.dostepnykampus.feature.map.campus.ShownPlan
import io.github.dostepnykampus.feature.map.navigation.MapFocus

/**
 * Karta albo arkusz nad mapą V1: budynek, którego plan trzyma, i dół mapy, który zasłania. Plan bierze z niego
 * wybrany budynek, a kamera dopełnienie od dołu, więc środek mapy to środek części widocznej nad arkuszem.
 *
 * Wymagania: US16.1, US16.2, US2.8, US18.1, BR-G1
 *
 * @property building symbol budynku trzymającego plan; `null`, gdy wyborem planu rządzi środek mapy
 * @property cover wysokość zasłoniętego dołu mapy w pikselach; 0 bez dopełnienia kamery
 * @property wayCover wysokość karty sali zmierzona już z drogą w pikselach; 0 bez takiego pomiaru albo pod innym
 *   arkuszem
 */
internal data class MapPanel(val building: String? = null, val cover: Int = 0, val wayCover: Int = 0)

/**
 * Arkusz nad mapą przy zaznaczeniu [focus]. Otwarty arkusz wejścia albo znaku planu budynku [sheetBuilding] trzyma
 * ten budynek i zasłania dół mapy na [sheetCover] pikseli; bez niego dół zasłania karta sali na mapie, wysoka na
 * [roomCover]. Bez takiego arkusza budynek trzyma zaznaczenie według [selectedBuilding], także karta sali;
 * zaznaczone wejście z zamkniętym arkuszem już nic nie trzyma.
 *
 * Wymagania: US16.1, US16.2, US2.8, US3.3, US18.1, US18.2, BR-G1
 *
 * @param focus zaznaczenie z trasy
 * @param sheetBuilding symbol budynku otwartego arkusza wejścia albo znaku planu; `null` bez takiego arkusza
 * @param sheetCover wysokość tego arkusza zasłaniającego dół mapy w pikselach; 0 bez dopełnienia
 * @param roomCover wysokość karty sali na mapie w pikselach; 0 bez karty albo z kartą na cały ekran
 * @param roomWayCover wysokość tej karty zmierzona już z drogą; 0 przed takim pomiarem
 */
internal fun mapPanel(
    focus: MapFocus?,
    sheetBuilding: String?,
    sheetCover: Int,
    roomCover: Int = 0,
    roomWayCover: Int = 0,
): MapPanel =
    MapPanel(
        building = sheetBuilding ?: selectedBuilding(focus).takeUnless { focus is MapFocus.Entrance },
        cover = sheetCover.takeIf { it > 0 } ?: roomCover,
        wayCover = roomWayCover.takeIf { sheetCover <= 0 } ?: 0,
    )

/**
 * Znak planu w pomieszczeniu [id] stoi na pokazanym piętrze tego planu. Bez planu albo na innym piętrze znaku nie
 * widać, więc jego arkusz się zamyka.
 *
 * Wymagania: US16.1, US2.8
 */
internal fun ShownPlan?.showsSign(id: String): Boolean =
    this?.run { rooms[floor]?.rooms.orEmpty().any { it.id == id } } == true

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
