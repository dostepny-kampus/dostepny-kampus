/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import android.graphics.Bitmap
import kotlinx.serialization.json.JsonElement

/** Styl, który zapisuje dane źródeł i dodane obrazy, a filtry, barwy, stany obiektów i powiększenia pomija. */
internal class RecordingStyleTarget : MapStyleTarget {
    /** Ostatnie dane każdego źródła po nazwie źródła. */
    val data = mutableMapOf<String, String>()

    /** Nazwy źródeł w kolejności podania danych. */
    val sources = mutableListOf<String>()

    /** Nazwy dodanych obrazów w kolejności dodania. */
    val images = mutableListOf<String>()

    /** Nazwy źródeł i obrazów razem, w kolejności wywołań. */
    val calls = mutableListOf<String>()

    override fun filter(layer: String, filter: JsonElement) = Unit

    override fun paint(layer: String, property: String, value: JsonElement) = Unit

    override fun state(source: String, feature: Int, key: String, value: Boolean) = Unit

    override fun minZoom(layer: String, zoom: Double) = Unit

    override fun data(source: String, geoJson: String) {
        data[source] = geoJson
        sources += source
        calls += source
    }

    override fun image(name: String, image: Bitmap) {
        images += name
        calls += name
    }
}
