/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.tiles

import io.github.dostepnykampus.core.model.CampusBounds
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.tan

private val TILE_PATH = Regex("""/(\d+)/(\d+)/(\d+)\.pbf$""")
private const val DEGREES = 360.0
private const val HALF_DEGREES = 180.0

/**
 * Czy adres [url] to kafelek wektorowy `…/z/x/y.pbf`, który w całości leży poza każdym z prostokątów [areas].
 * Bez prostokątów żaden kafelek podkładu nie pasuje. Adres innego zasobu niż kafelek daje `false`.
 *
 * Wymagania: NFR-39
 */
internal fun isOutside(url: String, areas: List<CampusBounds>): Boolean {
    val match = TILE_PATH.find(url.substringBefore('?'))
    if (match == null) return false
    val (z, x, y) = match.destructured.toList().map(String::toInt)
    return areas.none { it.hasTile(z = z, x = x, y = y) }
}

private fun CampusBounds.hasTile(z: Int, x: Int, y: Int): Boolean {
    val count = 2.0.pow(z)
    val columns = column(west, count)..column(east, count)
    val rows = row(north, count)..row(south, count)
    return x in columns && y in rows
}

private fun column(longitude: Double, count: Double): Int = floor((longitude + HALF_DEGREES) / DEGREES * count).toInt()

private fun row(latitude: Double, count: Double): Int =
    floor((1.0 - asinh(tan(Math.toRadians(latitude))) / PI) / 2.0 * count).toInt()
