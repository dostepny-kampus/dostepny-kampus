/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

/** Tereny PWr z budynków katalogu; kontury „Budynek testowy” nie są danymi PWr. Wymagania: BR-G5 */
class CampusAreasTest {
    /**
     * Jest pięć terenów. Na pliku konturów z repozytorium wychodzą „Kampus główny” (78 budynków),
     * „Wittigowo” (9), „Bartoszowice” (3), „Długa” (4) i „T-9 Atol” (1), a każdy budynek ma jeden teren.
     *
     * Wymagania: BR-G5
     */
    @Test
    fun br_g5_five_areas_real_data() {
        val real = parseBuildingCatalog(File(System.getProperty("dostepnykampus.buildings")).readText())
        val areas = campusAreas(real.buildings)
        val sizes = areas.associate { it.name to it.symbols.size }
        val expected =
            mapOf("Kampus główny" to 78, "Wittigowo" to 9, "Bartoszowice" to 3, "Długa" to 4, "T-9 Atol" to 1)
        assertEquals(expected, sizes)
        assertEquals(real.buildings.size, areas.sumOf { it.buildings.size })
        assertEquals("Wittigowo", areas.single { "T-15" in it.symbols }.name)
    }

    /**
     * Budynek najwyżej 600 m od innego budynku terenu należy do tego terenu, także przez pośredni budynek;
     * budynek dalej niż 600 m od każdego tworzy osobny teren.
     *
     * Wymagania: BR-G5
     */
    @Test
    fun br_g5_chain_within_600_m() {
        val first = square(symbol = "A-1", south = LAT)
        val second = square(symbol = "T-1", south = LAT + SIDE + meters(value = 590.0))
        val third = square(symbol = "T-2", south = LAT + 2 * SIDE + meters(value = 1_180.0))
        val alone = square(symbol = "T-3", south = LAT + 3 * SIDE + meters(value = 1_790.0))
        val areas = campusAreas(listOf(alone, third, first, second))
        assertEquals(listOf(listOf("T-3"), listOf("T-2", "A-1", "T-1")), areas.map(CampusArea::symbols))
        assertEquals(listOf("T-3", "Kampus główny"), areas.map(CampusArea::name))
    }

    /** Liczba budynków terenu to liczba symboli; dwa kontury jednego symbolu liczą się raz. Wymagania: US1.7 */
    @Test
    fun us1_7_symbols_counted_once() {
        val outlines = listOf(square(symbol = "H-14", south = LAT), square(symbol = "H-14", south = LAT + 2 * SIDE))
        assertEquals(listOf(listOf("H-14")), campusAreas(outlines).map(CampusArea::symbols))
    }

    private fun meters(value: Double): Double = value / METERS_PER_DEGREE

    private fun square(symbol: String, south: Double): Building {
        val corners = listOf(LON to south, LON + SIDE to south, LON + SIDE to south + SIDE, LON to south + SIDE)
        val ring = (corners + corners.first()).map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }
        return Building(symbol = symbol, name = "Budynek testowy", outline = GeoPolygon(listOf(ring)))
    }

    private companion object {
        const val LON = 17.0
        const val LAT = 51.0
        const val SIDE = 0.0005
        const val METERS_PER_DEGREE = 111_195.0
    }
}
