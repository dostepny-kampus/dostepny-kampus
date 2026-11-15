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

    fun value(index: Int) = evaluate(expression = arguments[index], properties = properties, zoom = zoom)

    fun all(test: (Int) -> Boolean) = arguments.indices.all(test)

    return when (name) {
        "get" -> properties[arguments[0].jsonPrimitive.content] ?: JsonNull
        "has" -> JsonPrimitive(arguments[0].jsonPrimitive.content in properties)
        "zoom" -> JsonPrimitive(zoom)
        "!" -> JsonPrimitive(!value(0).jsonPrimitive.boolean)
        "==" -> JsonPrimitive(value(0) == value(1))
        "!=" -> JsonPrimitive(value(0) != value(1))
        ">=" -> JsonPrimitive(value(0).jsonPrimitive.double >= value(1).jsonPrimitive.double)
        "any" -> JsonPrimitive(!all { !value(it).jsonPrimitive.boolean })
        "all" -> JsonPrimitive(all { value(it).jsonPrimitive.boolean })
        else -> error("Wyrażenie $name poza filtrami barier")
    }
}
