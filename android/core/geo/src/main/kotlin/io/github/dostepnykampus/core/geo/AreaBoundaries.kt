/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoPoint
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Geometry

/**
 * Granice terenów PWr z pliku `data/areas/pwr-areas.geojson` wbudowanego w aplikację, po nazwie terenu (właściwość
 * `name`, ta sama co w [campusAreas]); czytane przy pierwszym użyciu.
 *
 * Wymagania: BR-G5, US1.7
 */
public val PwrAreaBoundaries: Map<String, GeoArea> by lazy { parseAreaBoundaries(AREAS_GEOJSON) }

/**
 * Granice terenów PWr z pliku w formacie `data/areas/pwr-areas.geojson`, bez sieci, po nazwie terenu (właściwość
 * `name`, ta sama co w [campusAreas]). Granica to wielokąt albo kilka wielokątów.
 *
 * Wymagania: BR-G5, US1.7
 *
 * @param geoJson treść pliku GeoJSON
 * @throws IllegalArgumentException gdy teren nie ma nazwy albo jego granica nie jest wielokątem
 */
public fun parseAreaBoundaries(geoJson: String): Map<String, GeoArea> =
    FeatureCollection.fromJson<Geometry?, JsonObject?>(geoJson).features.associate { feature ->
        val name =
            feature.properties
                ?.get("name")
                ?.run { jsonPrimitive.contentOrNull }
        requireNotNull(name) { "Teren bez nazwy w pliku granic terenów" }
        name to feature.geometry.toArea()
    }

/**
 * Teren, na którym wolno złożyć zgłoszenie: punkt należy do niego wewnątrz granicy albo na jej linii, jak `ST_Covers`
 * na serwerze.
 *
 * Wymagania: BR-G5, US4.9
 */
public fun interface CampusGrounds {
    /** Czy [point] leży wewnątrz granicy terenu albo na jej linii. */
    public fun covers(point: GeoPoint): Boolean
}

/** Teren złożony z granic [boundaries]: punkt należy do niego, gdy leży na którymkolwiek z nich. Wymagania: BR-G5 */
public fun campusGrounds(boundaries: Map<String, GeoArea>): CampusGrounds =
    CampusGrounds { point -> boundaries.values.any { it.covers(point) } }

/** Teren PWr z granic [PwrAreaBoundaries]. Wymagania: BR-G5, US4.9 */
public val PwrGrounds: CampusGrounds by lazy { campusGrounds(PwrAreaBoundaries) }
