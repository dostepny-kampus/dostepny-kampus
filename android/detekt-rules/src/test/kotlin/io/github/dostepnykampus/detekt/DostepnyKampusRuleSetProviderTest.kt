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

/** detekt wczytuje reguły projektu przez `ServiceLoader`, więc wpis w `META-INF/services` musi je wskazywać (#873). */
class DostepnyKampusRuleSetProviderTest {
    @Test
    fun `detekt znajduje reguły`() {
        val rules =
            ServiceLoader
                .load(RuleSetProvider::class.java)
                .single()
                .instance()
        assertEquals("dostepnykampus" to listOf("NoLineComment"), rules.id.value to rules.rules.keys.map { it.value })
    }
}
