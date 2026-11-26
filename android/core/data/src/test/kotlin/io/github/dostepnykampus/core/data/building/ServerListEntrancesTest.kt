/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.building

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceAssessment
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.network.building.ApiBuilding
import io.github.dostepnykampus.core.network.building.BuildingList
import io.github.dostepnykampus.core.network.building.DetailLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Wejścia katalogu mapy z listy budynków serwera i z jej kopii w telefonie; plik konturów zostaje, gdy lista nie ma
 * żadnego wejścia.
 *
 * Wymagania: US1.4, US17.1, BR-24, BR-27, NFR-10
 */
class ServerListEntrancesTest {
    /**
     * Wiersz: lista serwera z wejściem tylko z planów PWr (`pwr/…`) i z oznaczeniem z planów → katalog mapy ma
     *   wejścia z listy z ich oznaczeniem i położeniem, bez wejść z pliku.
     *
     * Wymagania: US1.4, BR-24, BR-27
     */
    @Test
    fun takesEntrancesFromServerList() {
        val catalog = FILE.withServerList(listOf(OSM_ENTRANCE, PWR_ENTRANCE))
        assertEquals(listOf(OSM_ENTRANCE, PWR_ENTRANCE), catalog.entrances, "wejścia z listy serwera")
    }

    /**
     * Wiersz: lista serwera bez żadnego wejścia (stary serwer albo stara kopia) → wejścia z pliku podanych budynków.
     *
     * Wymagania: NFR-10
     */
    @Test
    fun keepsFileEntrancesWithoutList() {
        val catalog = FILE.withServerList(emptyList())
        assertEquals(listOf(FILE_ENTRANCE), catalog.entrances, "wejścia z pliku")
    }

    /**
     * Wiersz: lista z wejściami zapisana w kopii i odczytana bez sieci → te same wejścia z nazwą nadaną przez
     *   import, oznaczeniem, dniem i źródłem, piętrem, opisem dojścia i szerokością drzwi.
     *
     * Wymagania: NFR-10
     */
    @Test
    fun copiesEntrancesToPhone() {
        val list = list(listOf(PWR_ENTRANCE, OSM_ENTRANCE))
        assertEquals(list, list.toCacheJson().toCachedBuildingList(), "lista z wejściami z kopii")
    }

    /**
     * Wiersz: kopia zapisana przed wejściami w liście → budynki bez wejść, bez błędu odczytu.
     *
     * Wymagania: NFR-10
     */
    @Test
    fun readsCopyWithoutEntrances() {
        val old =
            """{"source":"© serwer","buildings":[{"symbol":"A-1","name":"Budynek A-1","detailLevel":"BASIC",""" +
                """"outline":{"type":"Polygon","coordinates":[[[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,0.0]]]}}]}"""
        assertEquals(list(emptyList()), old.toCachedBuildingList(), "stara kopia bez wejść")
    }

    private fun BuildingCatalog.withServerList(entrances: List<Entrance>): BuildingCatalog =
        withServerList(list(entrances))

    private companion object {
        val OUTLINE =
            GeoPolygon(
                rings =
                    listOf(
                        listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 0.0).map { (lon, lat) ->
                            GeoPoint(longitude = lon, latitude = lat)
                        },
                    ),
            )
        val BUILDING = Building(symbol = "A-1", name = "Budynek A-1", outline = OUTLINE)
        val FILE_ENTRANCE =
            Entrance(
                id = "node/1",
                buildingSymbol = "A-1",
                name = null,
                isMain = true,
                type = "main",
                location = GeoPoint(longitude = 0.5, latitude = 0.0),
                marking = EntranceMarking.NO_DATA,
            )
        val OSM_ENTRANCE =
            FILE_ENTRANCE.copy(
                marking = EntranceMarking.ACCESSIBLE,
                assessment = EntranceAssessment(LocalDate.of(2026, 11, 2), AssessmentSource.WALKTHROUGH),
            )
        val PWR_ENTRANCE =
            Entrance(
                id = "pwr/s00000001",
                buildingSymbol = "A-1",
                name = "Wejście",
                isNameGenerated = true,
                isMain = false,
                type = null,
                location = GeoPoint(longitude = 0.0, latitude = 0.5),
                marking = EntranceMarking.INACCESSIBLE,
                assessment = EntranceAssessment(LocalDate.of(2026, 11, 1), AssessmentSource.PWR_PLANS),
                floorId = "P0",
                approach = "Do wejścia prowadzą schody.",
                doorWidthCm = 90,
            )
        val FILE = BuildingCatalog(buildings = listOf(BUILDING), entrances = listOf(FILE_ENTRANCE), attribution = "")

        fun list(entrances: List<Entrance>): BuildingList =
            BuildingList(
                source = "© serwer",
                buildings = listOf(ApiBuilding(BUILDING, DetailLevel.BASIC, entrances)),
            )
    }
}
