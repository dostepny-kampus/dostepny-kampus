/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Plik próbny reguły tekstu JSON (#890): każde wywołanie w [text] ma dać błąd detekt; znika przed scaleniem. */
internal fun probeJsonText(json: Json, text: String, element: JsonObject): String {
    val parsed = json.parseToJsonElement(text)
    val decoded = json.decodeFromString(JsonElement.serializer(), text)
    val encoded = json.encodeToString(JsonElement.serializer(), decoded)
    return encoded + element.toString() + parsed.toString()
}
