/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.featureCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

/** Teksty GeoJSON źródeł stylu z danymi katalogu i barier, po nazwie źródła; reszta źródeł nie zależy od danych. */
internal fun CampusMapData.sourceTexts(): Map<String, String> =
    mapOf(
        BUILDINGS_SOURCE to buildings.asStyleText(),
        LABELS_SOURCE to labelPoints.asStyleText(),
        ENTRANCES_SOURCE to entrances.asStyleText(),
        BARRIERS_SOURCE to barriers.asStyleText(),
    )

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
