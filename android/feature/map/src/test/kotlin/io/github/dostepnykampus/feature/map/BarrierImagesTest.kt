/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** Obrazy barier dla mapy V1: znak i słowo każdej uciążliwości (US1.3, NFR-21). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BarrierImagesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz: V1, bariery jako znak uciążliwości i słowo obok znaku (US1.3, NFR-21). */
    @Test
    fun us1_3_sign_and_word() {
        val images = barrierImages(images(words = listOf("Utrudnienie", "Tylko z pomocą", "Blokada")))

        val names = Severity.entries.flatMap { listOf(barrierSignImage(it), barrierWordImage(it)) }
        assertEquals(names.toSet(), images.keys)
        val signs = Severity.entries.map { images.getValue(barrierSignImage(it)) }
        assertEquals(listOf(SIGN_PX), signs.map { it.width }.distinct())
        assertFalse(signs.first().sameAs(signs.last()))
    }

    @Test
    fun us1_3_signs_without_words() {
        val images = barrierImages(images(words = emptyList()))

        assertEquals(Severity.entries.map(::barrierSignImage).toSet(), images.keys)
    }

    private fun images(words: List<String>): MapImages {
        var colors: MapColors? = null
        compose.setContent { DostepnyKampusTheme { colors = DostepnyKampusTheme.mapColors } }
        compose.waitForIdle()
        return MapImages(colors = checkNotNull(colors), density = DENSITY, labelSize = LABEL_PX, words = words)
    }

    private companion object {
        const val DENSITY = 2f
        const val LABEL_PX = 28f
        const val SIGN_PX = BARRIER_SIGN_DP * 2
    }
}
