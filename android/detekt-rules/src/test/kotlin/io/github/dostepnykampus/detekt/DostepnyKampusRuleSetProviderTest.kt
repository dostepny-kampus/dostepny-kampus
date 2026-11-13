/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.detekt

import dev.detekt.api.RuleSetProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.ServiceLoader

/** detekt znajduje reguły projektu przez `ServiceLoader` z wpisu w `META-INF/services`. */
class DostepnyKampusRuleSetProviderTest {
    @Test
    fun `detekt znajduje reguły`() {
        val rules =
            ServiceLoader
                .load(RuleSetProvider::class.java)
                .single()
                .instance()
        assertEquals(
            "dostepnykampus" to
                listOf(
                    "NoLineComment",
                    "NoHistoryNumberInKDoc",
                    "KDocDescriptionLength",
                    "RequirementIdOutsideLine",
                ),
            rules.id.value to rules.rules.keys.map { it.value },
        )
    }
}
