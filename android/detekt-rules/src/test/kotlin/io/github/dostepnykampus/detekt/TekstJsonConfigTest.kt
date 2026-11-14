/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.detekt

import dev.detekt.core.config.YamlConfig
import dev.detekt.rules.style.ForbiddenMethodCall
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * Egzemplarz `ForbiddenMethodCall/tekst-json` z `config/detekt/detekt.yml` na krótkich źródłach: zgłasza zamianę
 * tekstu JSON z jawnym i z niejawnym odbiorcą.
 */
class TekstJsonConfigTest {
    private val rule =
        ForbiddenMethodCall(
            Files
                .newBufferedReader(Path.of("..", "config", "detekt", "detekt.yml"))
                .use(YamlConfig::load)
                .subConfig("style")
                .subConfig("ForbiddenMethodCall/tekst-json"),
        )

    @Test
    fun `jawny odbiorca to błąd`() {
        assertEquals(
            listOf(6),
            lines(
                "fun y(json: Json, body: String): String =",
                "    json.decodeFromString(String.serializer(), body)",
            ),
        )
    }

    @Test
    fun `niejawny odbiorca w rozszerzeniu Json to błąd`() {
        assertEquals(
            listOf(6),
            lines(
                "fun Json.x(body: String): String =",
                "    decodeFromString(String.serializer(), body)",
            ),
        )
    }

    @Test
    fun `serializer wygenerowany wtyczką w rozszerzeniu Json to błąd`() {
        assertEquals(
            listOf(7),
            lines(
                "@Serializable class B(val a: Int)",
                "fun Json.x(body: String): B =",
                "    decodeFromString(B.serializer(), body)",
            ),
        )
    }

    @Test
    fun `serializer wygenerowany wtyczką z jawnym odbiorcą to błąd`() {
        assertEquals(
            listOf(7),
            lines(
                "@Serializable class B(val a: Int)",
                "fun y(json: Json, body: String): B =",
                "    json.decodeFromString(B.serializer(), body)",
            ),
        )
    }

    private fun lines(vararg body: String): List<Int> =
        rule
            .lintWithContext(createEnvironment(), (HEADER + body).joinToString(separator = "\n"))
            .map { it.entity.location.source.line }
            .distinct()

    private companion object {
        val HEADER =
            listOf(
                "package a",
                "import kotlinx.serialization.Serializable",
                "import kotlinx.serialization.builtins.serializer",
                "import kotlinx.serialization.json.Json",
            )
    }
}
