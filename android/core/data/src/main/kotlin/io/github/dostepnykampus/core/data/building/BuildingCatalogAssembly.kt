/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.building

import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.network.building.BuildingList
import io.github.dostepnykampus.core.network.building.DetailLevel

private val NATURAL_ORDER =
    compareBy<Building>(
        { it.symbol.substringBefore('-') },
        { it.symbol.filter(Char::isDigit).toIntOrNull() },
        { it.symbol },
    )

/** Katalog dla odwiedzających z budynkami w kolejności naturalnej i wejściami dla odwiedzających. Wymagania: BR-25 */
internal fun BuildingCatalog.forVisitors(): BuildingCatalog =
    copy(
        buildings = buildings.sortedWith(NATURAL_ORDER),
        entrances = entrances.filter { it.isForVisitors },
    )

/**
 * Katalog z listy serwera na tle katalogu z pliku: kontury, nazwy, atrybucja i budynki szczegółowe z serwera,
 * wejścia z pliku, ale tylko budynków, które serwer podał; wynik w kolejności i z wejściami dla odwiedzających.
 *
 * Wymagania: US1.2, BR-G1
 */
internal fun BuildingCatalog.withServerList(server: BuildingList): BuildingCatalog {
    val symbols = server.buildings.map { item -> item.building.symbol }.toSet()
    val detailed = server.buildings.filter { it.detailLevel == DetailLevel.DETAILED }.map { it.building.symbol }
    val outlines =
        copy(
            buildings = server.buildings.map { item -> item.building },
            entrances = entrances.filter { entrance -> entrance.buildingSymbol in symbols },
            attribution = server.source,
            detailed = detailed.toSet(),
        )
    return outlines.forVisitors()
}
