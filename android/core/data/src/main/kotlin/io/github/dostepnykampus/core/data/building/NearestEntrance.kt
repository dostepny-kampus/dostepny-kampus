/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.building

import android.database.sqlite.SQLiteException
import io.github.dostepnykampus.core.geo.building.nearestEntrance
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.EntranceDistance
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/**
 * Najbliższe wejście budynku dla punktu zgłoszenia, z katalogu budynków. Brak katalogu w telefonie i bez sieci daje
 * brak wejścia, a nie błąd.
 *
 * Wymagania: US7.4, US8.4, NFR-19
 *
 * @param catalog katalog budynków z wejściami
 */
public class NearestEntrance
    @Inject
    public constructor(
        private val catalog: BuildingCatalogRepository,
    ) {
        /**
         * Najbliższe wejście budynku [buildingSymbol] do [point].
         *
         * @param buildingSymbol symbol budynku zgłoszenia; `null` dla zgłoszenia poza budynkiem
         * @param point punkt zgłoszenia
         * @return wejście z odległością; `null` bez budynku, jego wejść albo katalogu
         */
        public suspend fun of(buildingSymbol: String?, point: GeoPoint): EntranceDistance? {
            val found = if (buildingSymbol == null) null else catalogOrNull()
            val own = found?.entrances.orEmpty().filter { it.buildingSymbol == buildingSymbol }
            return nearestEntrance(point = point, entrances = own)
        }

        private suspend fun catalogOrNull(): BuildingCatalog? =
            try {
                catalog.catalog()
            } catch (error: IOException) {
                Timber.w("Katalog budynków dla najbliższego wejścia niedostępny: %s", error.javaClass.simpleName)
                null
            } catch (error: SQLiteException) {
                Timber.w("Katalog budynków dla najbliższego wejścia niedostępny: %s", error.javaClass.simpleName)
                null
            }
    }
