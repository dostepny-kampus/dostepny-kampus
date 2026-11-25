/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.photo

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Próg ostrzeżenia o miejscu w telefonie na C4. Wymagania: NFR-7 */
class PhotoSpaceTest {
    @Test
    fun lowBelowHundredMegabytes() {
        assertTrue(isLowSpace(availableBytes = 99_999_999L), "mniej niż 100 MB")
    }

    @Test
    fun enoughFromHundredMegabytes() {
        assertFalse(isLowSpace(availableBytes = 100_000_000L), "dokładnie 100 MB to jeszcze nie mało")
    }

    @Test
    fun readsPhoneSpace() {
        assertTrue(StoragePhotoSpace { 1L }.isLow(), "prawie pełny telefon")
        assertFalse(StoragePhotoSpace { 1_000_000_000L }.isLow(), "dużo miejsca")
    }

    @Test
    fun unknownSpaceIsNotLow() {
        assertFalse(StoragePhotoSpace { error("bez dostępu") }.isLow(), "błąd odczytu nie ostrzega")
    }
}
