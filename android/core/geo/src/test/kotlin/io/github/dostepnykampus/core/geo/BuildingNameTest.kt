/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** Nazwa pod symbolem w nagłówku V2 nie powtarza symbolu (US2, #1038). */
class BuildingNameTest {
    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "A-1 / Budynek główny | Budynek główny",
            "T-7 Dom doktoranta | Dom doktoranta",
            "P-22 - Hala Widowiskowo-Sportowa | Hala Widowiskowo-Sportowa",
            """T-16 "Tower" | "Tower"""",
            """T-3 "Straszny Dwór" | "Straszny Dwór"""",
            "M-6 bis | M-6 bis",
            """Dom Studencki T-6 "Alcatraz" | Dom Studencki T-6 "Alcatraz"""",
            "Budynek testowy | Budynek testowy",
        ],
    )
    fun us2_name_without_own_symbol(name: String, expected: String) {
        assertEquals(expected, buildingName(name))
    }
}
