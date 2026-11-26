/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.review

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Powód odrzucenia „nieaktualne” wysłany przez Weryfikatora i rozpoznany u autora.
 *
 * Wymagania: BR-23, BR-24
 */
class RejectionReasonsTest {
    @Test
    fun recognisesOutdated() {
        assertTrue(RejectionReasons.isOutdated(RejectionReasons.OUTDATED), "stała protokołu")
        assertTrue(RejectionReasons.isOutdated(" Nieaktualne "), "inna wielkość liter i odstępy")
    }

    @Test
    fun ignoresOtherReasons() {
        assertFalse(RejectionReasons.isOutdated("Zdjęcie nieostre"), "własny powód")
        assertFalse(RejectionReasons.isOutdated(null), "bez powodu")
    }
}
