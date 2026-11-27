/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.map

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/** Biblioteka natywna MapLibre: bez niej mapa nie powstaje. Wymagania: NFR-11 */
class NativeMapTest {
    /** W teście JVM biblioteki nie ma: wczytanie zwraca `false`. */
    @Test
    fun reportsMissingLibrary() {
        assertFalse(loadNativeMap(), "wczytanie bez biblioteki")
    }
}
