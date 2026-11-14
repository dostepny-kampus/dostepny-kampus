/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.testing

import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.server.ServerBuildings
import io.github.dostepnykampus.core.data.server.ServerReply
import kotlin.coroutines.CoroutineContext

/**
 * Katalog budynków, który pyta serwer przez [list], a bez odpowiedzi czyta plik przez [readGeoJson].
 *
 * TODO(#1651): testy funkcji wołają BuildingCatalogRepository wprost, a moduł znika.
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
): BuildingCatalogRepository = BuildingCatalogRepository(ioContext, list, nowMillis, readGeoJson)
