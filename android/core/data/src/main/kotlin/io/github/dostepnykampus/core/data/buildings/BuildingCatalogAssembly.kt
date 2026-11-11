/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.network.BuildingList

private val NATURAL_ORDER =
    compareBy<Building>(
        { it.symbol.substringBefore('-') },
        { it.symbol.filter(Char::isDigit).toIntOrNull() },
        { it.symbol },
    )

/** Katalog dla odwiedzających z budynkami w kolejności naturalnej i wejściami zgodnymi z BR-25. */
internal fun BuildingCatalog.forVisitors(): BuildingCatalog =
    copy(
        buildings = buildings.sortedWith(NATURAL_ORDER),
        entrances = entrances.filter { it.isForVisitors },
    )

/**
 * Katalog z listy serwera na tle katalogu z pliku (US1.2): kontury, nazwy i atrybucja z serwera, wejścia z pliku,
 * ale tylko budynków, które serwer podał; wynik w kolejności i z wejściami dla odwiedzających.
 */
internal fun BuildingCatalog.withServerList(server: BuildingList): BuildingCatalog {
    val symbols = server.buildings.map { item -> item.building.symbol }.toSet()
    val outlines =
        copy(
            buildings = server.buildings.map { item -> item.building },
            entrances = entrances.filter { entrance -> entrance.buildingSymbol in symbols },
            attribution = server.source,
        )
    return outlines.forVisitors()
}
