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
import tools.jackson.databind.JsonNode
import java.nio.file.Files
import java.nio.file.Path

/**
 * Kontrakt `api/openapi.yaml` (decyzja 122) jako drzewo i walidator schematów, dialekt OpenAPI 3.1 jak na serwerze.
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

    /** Węzeł pod wskaźnikiem JSON, z podążeniem za `$ref`; brakujący węzeł ma `isMissingNode`. */
    fun node(pointer: String): JsonNode = resolve(tree.at(pointer))

    /** Właściwości schematu, także z `allOf` i `oneOf`. */
    fun properties(node: JsonNode): Map<String, JsonNode> =
        parts(node).flatMap { it.path("properties").properties() }.associate { it.key to it.value }

    /** Nazwy pól wymaganych schematu, także z `allOf` i `oneOf`. */
    fun required(node: JsonNode): Set<String> =
        parts(node)
            .flatMap { part -> part.path("required").toList().map { name -> name.asString() } }
            .toSet()

    /** Naruszenia schematu pod wskaźnikiem przez [json]; puste dla zgodnej treści. */
    fun violations(pointer: String, json: String): List<String> =
        registry
            .getSchema(SchemaLocation.of("$LOCATION#$pointer"))
            .validate(json, InputFormat.JSON)
            .map { "${it.instanceLocation} ${it.message}" }

    private fun resolve(node: JsonNode): JsonNode =
        if (node.has(REF)) resolve(tree.at(node.get(REF).asString().removePrefix("#"))) else node

    private fun parts(node: JsonNode): List<JsonNode> {
        val schema = resolve(node)
        return listOf(schema) + (schema.path("allOf").toList() + schema.path("oneOf").toList()).flatMap(::parts)
    }

    private companion object {
        const val LOCATION = "https://dostepny-kampus.test/openapi.yaml"
        const val REF = "\$ref"
    }
}
