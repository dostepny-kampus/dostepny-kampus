/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive

/**
 * Wynik filtra warstwy [filter] dla obiektu o właściwościach [properties] przy powiększeniu [zoom]. Zna tylko
 * wyrażenia, których używają filtry barier; inne kończą test błędem.
 */
internal fun passes(filter: JsonElement, properties: JsonObject, zoom: Double = 0.0): Boolean =
    evaluate(expression = filter, properties = properties, zoom = zoom).jsonPrimitive.boolean

private fun evaluate(expression: JsonElement, properties: JsonObject, zoom: Double): JsonElement {
    if (expression !is JsonArray) return expression
    val name = expression.first().jsonPrimitive.content
    val arguments = expression.drop(1)
    return when (name) {
        "get" -> properties[arguments[0].jsonPrimitive.content] ?: JsonNull
        "has" -> JsonPrimitive(arguments[0].jsonPrimitive.content in properties)
        "zoom" -> JsonPrimitive(zoom)
        else -> JsonPrimitive(logic(name = name, values = arguments.map { evaluate(it, properties, zoom) }))
    }
}

private fun logic(name: String, values: List<JsonElement>): Boolean =
    when (name) {
        "!" -> !values[0].jsonPrimitive.boolean
        "==" -> values[0] == values[1]
        "!=" -> values[0] != values[1]
        ">=" -> values[0].jsonPrimitive.double >= values[1].jsonPrimitive.double
        "any" -> values.any { it.jsonPrimitive.boolean }
        "all" -> values.all { it.jsonPrimitive.boolean }
        else -> error("Wyrażenie $name poza filtrami barier")
    }
