/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pakiet kodu produkcyjnego ma najwyżej [LIMIT] plików Kotlin. Pakiety ponad limitem są na liście wyjątków
 * z numerem issue, a lista może tylko maleć.
 */
class PackageSizeArchitectureTest {
    /** Pakiety spoza listy wyjątków mieszczą się w limicie. */
    @Test
    fun infra_package_fits_limit() {
        val found = oversized(Konsist.scopeFromProduction().files)
        assertEquals(emptyMap<String, Int>(), found - OVERSIZED.keys, HINT)
    }

    /** Wpis listy wyjątków dla pakietu, który mieści się w limicie, czerwieni test. */
    @Test
    fun infra_package_list_shrinks() {
        val found = oversized(Konsist.scopeFromProduction().files)
        assertEquals(emptySet<String>(), OVERSIZED.keys - found.keys)
    }

    /** Szesnasty plik w pakiecie łamie regułę, piętnaście plików nie. */
    @Test
    fun infra_package_rule_rejects(
        @TempDir fixtures: File,
    ) {
        val full = File(fixtures, "full").apply { mkdirs() }
        val fits = File(fixtures, "fits").apply { mkdirs() }
        (1..LIMIT + 1).forEach { File(full, "Full$it.kt").writeText(source("full", it)) }
        (1..LIMIT).forEach { File(fits, "Fits$it.kt").writeText(source("fits", it)) }
        val files = Konsist.scopeFromExternalDirectory(fixtures.absolutePath).files
        assertEquals(mapOf("$BASE.sample.full" to LIMIT + 1), oversized(files))
    }

    private fun source(name: String, index: Int): String = "package $BASE.sample.$name\nval $name$index = $index"

    private fun oversized(files: List<KoFileDeclaration>): Map<String, Int> =
        files
            .groupingBy { it.packagee?.name.orEmpty() }
            .eachCount()
            .filter { (name, count) -> name.isNotEmpty() && count > LIMIT }

    private companion object {
        const val BASE = "io.github.dostepnykampus"

        const val LIMIT = 15

        const val HINT = "Pakiet ma więcej niż $LIMIT plików; podziel go według ścieżki użytkownika albo tematu"

        val OVERSIZED =
            mapOf(
                "$BASE.feature.map" to 1330,
                "$BASE.feature.capture" to 1550,
            )
    }
}
