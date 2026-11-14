/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.testing

import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.buildings.ServerEntrances
import io.github.dostepnykampus.core.data.buildings.ServerEntrancesRepository
import io.github.dostepnykampus.core.network.ApiBuilding
import io.github.dostepnykampus.core.network.BuildingDetails
import io.github.dostepnykampus.core.network.BuildingList
import io.github.dostepnykampus.core.network.DetailLevel
import kotlin.coroutines.CoroutineContext

/**
 * Katalog budynków, który pyta serwer przez [list], a bez odpowiedzi czyta plik przez [readGeoJson].
 *
 * @param ioContext kontekst odczytu pliku i serwera
 * @param list odpowiedź serwera na listę budynków; może czekać albo nie wrócić
 * @param nowMillis zegar chwili pobrania listy
 * @param readGeoJson treść pliku konturów z pakietu aplikacji
 * @return repozytorium jak w aplikacji, bez kopii w bazie
 */
fun serverBuildingCatalog(
    ioContext: CoroutineContext,
    list: suspend () -> ServerReply<ServerBuildings>,
    nowMillis: () -> Long = System::currentTimeMillis,
    readGeoJson: () -> String,
): BuildingCatalogRepository =
    BuildingCatalogRepository(
        ioContext = ioContext,
        listBuildings = { list().toApiResult(::buildingList) },
        nowMillis = nowMillis,
        readGeoJson = readGeoJson,
    )

/**
 * Wejścia budynków z karty budynku z serwera.
 *
 * @param details odpowiedź serwera na kartę budynku o danym symbolu
 * @return wejścia jak w aplikacji
 */
fun serverEntrances(details: suspend (String) -> ServerReply<ServerCard>): ServerEntrances =
    ServerEntrancesRepository { symbol -> details(symbol).toApiResult(::buildingDetails) }

private fun buildingList(found: ServerBuildings): BuildingList =
    BuildingList(source = found.source, buildings = found.buildings.map { ApiBuilding(it, DetailLevel.BASIC) })

private fun buildingDetails(card: ServerCard): BuildingDetails =
    BuildingDetails(card.building, DetailLevel.BASIC, card.entrances)
