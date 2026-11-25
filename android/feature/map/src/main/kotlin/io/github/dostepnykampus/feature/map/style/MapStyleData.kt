/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.feature.map.camera.MapTrace
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.featureCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlin.coroutines.CoroutineContext

private val EMPTY_MAP_DATA =
    CampusMapData(
        buildings = featureCollection(emptyList()),
        labels = emptyList(),
        labelPoints = featureCollection(emptyList()),
        entrances = featureCollection(emptyList()),
        start = null,
    )

/**
 * Styl mapy jak [campusMapStyle] z pustymi źródłami konturów, podpisów, wejść i barier: nie zależy od katalogu ani
 * barier, więc nowe dane nie wczytują go od nowa. Dane idą do wczytanego stylu przez [showData].
 *
 * Wymagania: NFR-11
 */
internal fun campusMapShell(selection: MapSelection, colors: MapStyleColors, basemap: Basemap?): String =
    campusMapStyle(data = EMPTY_MAP_DATA, selection = selection, colors = colors, basemap = basemap)

/**
 * Teksty GeoJSON źródeł stylu z danymi katalogu, barier i ścieżek, po nazwie źródła; reszta źródeł nie zależy od
 * danych.
 */
internal fun CampusMapData.sourceTexts(): Map<String, String> =
    mapOf(
        BUILDINGS_SOURCE to geoJsonText(BUILDINGS_SOURCE) { buildings },
        LABELS_SOURCE to geoJsonText(LABELS_SOURCE) { labelPoints },
        ENTRANCES_SOURCE to geoJsonText(ENTRANCES_SOURCE) { entrances },
        BARRIERS_SOURCE to geoJsonText(BARRIERS_SOURCE) { barriers },
        PATHS_SOURCE to geoJsonText(PATHS_SOURCE) { paths },
    )

/**
 * Tekst GeoJSON źródła stylu [source] z danych od [build]. Przy włączonym śledzeniu budowa leży w sekcji
 * [MapTrace.geoJsonSection] tego źródła; [build] nie może zmienić wątku.
 *
 * Wymagania: NFR-57
 */
internal inline fun geoJsonText(source: String, build: () -> JsonElement): String =
    MapTrace.section(name = MapTrace.geoJsonSection(source)) { build().asStyleText() }

/**
 * Pokazuje [data] we wczytanym stylu z [campusMapShell]: dodaje obrazy podpisów spoza [labelled] i zastępuje dane
 * źródeł. Tekst danych i obrazy podpisów powstają w [worker], poza wątkiem głównym; MapLibre parsuje dane w tle.
 * Po wywołaniu styl rysuje to samo co [campusMapStyle] dla [data].
 *
 * Wymagania: NFR-11, US1.1
 */
internal suspend fun MapStyleTarget.showData(
    data: CampusMapData,
    images: MapImages,
    labelled: MutableSet<String>,
    worker: CoroutineContext = Dispatchers.Default,
) {
    val fresh = data.labels.filterNot(labelled::contains)
    val (texts, bitmaps) =
        withContext(worker) {
            data.sourceTexts() to fresh.associateWith { labelImage(it, images.colors, images.labelSize) }
        }
    bitmaps.forEach { (label, bitmap) -> image(label, bitmap) }
    labelled += fresh
    texts.forEach { (source, text) -> data(source, text) }
}
