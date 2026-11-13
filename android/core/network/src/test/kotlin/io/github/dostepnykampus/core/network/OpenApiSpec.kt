/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.dialect.Dialects
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import tools.jackson.databind.JsonNode
import java.nio.file.Files
import java.nio.file.Path

/**
 * Kontrakt `api/openapi.yaml` jako drzewo i walidator schematów, dialekt OpenAPI 3.1 jak na serwerze.
 * Brak pliku to błąd, więc zła ścieżka nie daje zielonego testu.
 *
 * @param path ścieżka kontraktu względem modułu
 */
internal class OpenApiSpec(path: Path) {
    private val registry =
        SchemaRegistry
            .withDialect(Dialects.getOpenApi31()) { it.schemas(mapOf(LOCATION to Files.readString(path))) }
            .getSchema(SchemaLocation.of(LOCATION))
            .schemaContext
            .schemaRegistry
    private val tree: JsonNode = registry.getSchema(SchemaLocation.of(LOCATION)).schemaNode

    fun node(pointer: String): JsonNode = resolve(tree.at(pointer))

    fun properties(node: JsonNode): Map<String, JsonNode> =
        parts(node).flatMap { it.path("properties").properties() }.associate { it.key to it.value }

    fun required(node: JsonNode): Set<String> =
        parts(node)
            .flatMap { part -> part.path("required").toList().map { name -> name.asString() } }
            .toSet()

    fun violations(pointer: String, json: String): List<String> =
        registry
            .getSchema(SchemaLocation.of("$LOCATION#$pointer"))
            .validate(json, InputFormat.JSON)
            .map { "${it.instanceLocation} ${it.message}" }

    fun resolve(node: JsonNode): JsonNode =
        if (node.has(REF)) resolve(tree.at(node.get(REF).asString().removePrefix("#"))) else node

    /** Minimalny JSON zgodny ze schematem: tylko pola wymagane, wartości przykładowe zgodne z typem. */
    fun sample(node: JsonNode): JsonElement {
        val schema = resolve(node)
        val fields = properties(schema)
        val first = schema.path("oneOf").toList().firstOrNull()
        return when {
            first != null -> sample(first)
            schema.has("const") -> JsonPrimitive(schema.get("const").asString())
            schema.has("enum") -> JsonPrimitive(schema.path("enum").get(0).asString())
            fields.isNotEmpty() -> JsonObject(required(schema).associateWith { sample(fields.getValue(it)) })
            else -> scalar(schema)
        }
    }

    private fun scalar(schema: JsonNode): JsonElement =
        when (schema.path("type").asString("")) {
            "array" -> JsonArray(List(schema.path("minItems").asInt(0)) { sample(schema.path("items")) })
            "integer" -> JsonPrimitive(1)
            "number" -> JsonPrimitive(1.0)
            "boolean" -> JsonPrimitive(true)
            else -> JsonPrimitive(TEXTS[schema.path("format").asString("")] ?: "x")
        }

    private fun parts(node: JsonNode): List<JsonNode> {
        val schema = resolve(node)
        return listOf(schema) + (schema.path("allOf").toList() + schema.path("oneOf").toList()).flatMap(::parts)
    }

    private companion object {
        const val LOCATION = "https://dostepny-kampus.test/openapi.yaml"
        const val REF = "\$ref"
        val TEXTS = mapOf("date-time" to "2026-11-04T09:30:00Z", "date" to "2026-11-04")
    }
}
