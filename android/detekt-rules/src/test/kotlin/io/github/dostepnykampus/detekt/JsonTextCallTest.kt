/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.detekt

import dev.detekt.api.Config
import dev.detekt.api.Finding
import dev.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Reguła [JsonTextCall] na krótkich źródłach: zgłasza zamianę tekstu JSON bez względu na typy. */
class JsonTextCallTest {
    private val rule = JsonTextCall(Config.empty)

    @Test
    fun rejectsUntypedRead() {
        assertEquals(
            listOf(2),
            lines(
                "fun Json.read(body: String): Int {",
                "    val list = decodeFromString(Model.serializer(), body)",
                "    return 1",
                "}",
            ),
            "Reguła przepuściła odczyt tekstu JSON do zmiennej bez typu",
        )
    }

    @Test
    fun rejectsWriteAndParse() {
        assertEquals(
            listOf(1, 2),
            lines(
                "val text = json.encodeToString(Model.serializer(), model)",
                "val tree = json.parseToJsonElement(text)",
            ),
            "Reguła przepuściła zapis albo parsowanie tekstu JSON",
        )
    }

    @Test
    fun acceptsOtherCalls() {
        assertEquals(
            emptyList<Int>(),
            lines(
                "val tree = json.decodeFromJsonElement(Model.serializer(), element)",
                "val text = decodeContract(s, body)",
            ),
            "Reguła zgłosiła wywołanie, które nie zamienia tekstu JSON",
        )
    }

    private fun lines(vararg source: String): List<Int> =
        rule.lint(source.joinToString(separator = "\n"), compile = false).map(::line)

    private fun line(finding: Finding): Int = finding.entity.location.source.line
}
