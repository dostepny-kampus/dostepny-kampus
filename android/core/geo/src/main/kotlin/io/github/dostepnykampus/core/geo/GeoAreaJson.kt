/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPolygon
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.MultiPolygon
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.Position

/**
 * Kontur budynku jako pole JSON: geometria GeoJSON (`Polygon` albo `MultiPolygon`) i [GeoArea]
 * bez zapisu tekstowego po drodze (#828). Zła geometria daje [IllegalArgumentException]
 * w miejscu odczytu odpowiedzi albo kopii.
 */
object GeoAreaJson : KSerializer<GeoArea> {
    private val geometry = Geometry.serializer()

    override val descriptor: SerialDescriptor = geometry.descriptor

    override fun deserialize(decoder: Decoder): GeoArea = geometry.deserialize(decoder).toArea()

    override fun serialize(encoder: Encoder, value: GeoArea) {
        geometry.serialize(encoder, value.toGeometry())
    }
}

private fun GeoArea.toGeometry(): Geometry =
    when (this) {
        is GeoPolygon -> Polygon(positions(this))
        is GeoMultiPolygon -> MultiPolygon(polygons.map(::positions))
    }

private fun positions(polygon: GeoPolygon): List<List<Position>> =
    polygon.rings.map { ring ->
        ring.map { point -> Position(longitude = point.longitude, latitude = point.latitude) }
    }
