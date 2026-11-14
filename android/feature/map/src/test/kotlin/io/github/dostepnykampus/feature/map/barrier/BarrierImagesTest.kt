/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.barrier

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.feature.map.MapImages
import io.github.dostepnykampus.feature.map.style.BARRIER_SIGN_DP
import io.github.dostepnykampus.feature.map.style.CHOSEN_BARRIER_DP
import io.github.dostepnykampus.feature.map.style.barrierSignImage
import io.github.dostepnykampus.feature.map.style.barrierWordImage
import io.github.dostepnykampus.feature.map.style.chosenBarrierImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** Obrazy barier dla mapy V1: znak i słowo każdej uciążliwości. Wymagania: US1.3, NFR-21 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BarrierImagesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz: V1, bariery jako znak uciążliwości i słowo obok znaku. Wymagania: US1.3, NFR-21 */
    @Test
    fun us1_3_sign_and_word() {
        val images = barrierImages(images(words = listOf("Utrudnienie", "Tylko z pomocą", "Blokada")))

        val names =
            Severity.entries.flatMap { listOf(barrierSignImage(it), barrierWordImage(it), chosenBarrierImage(it)) }
        assertEquals(names.toSet(), images.keys)
        val signs = Severity.entries.map { images.getValue(barrierSignImage(it)) }
        assertEquals(listOf(SIGN_PX), signs.map { it.width }.distinct())
        assertFalse(signs.first().sameAs(signs.last()))
    }

    @Test
    fun us1_3_signs_without_words() {
        val images = barrierImages(images(words = emptyList()))

        val names = Severity.entries.flatMap { listOf(barrierSignImage(it), chosenBarrierImage(it)) }
        assertEquals(names.toSet(), images.keys)
    }

    /**
     * Wiersz: V1, znak wybranej bariery jest osobnym, większym obrazem dla każdej uciążliwości.
     *
     * Wymagania: US1.5, NFR-21
     */
    @Test
    fun us1_5_chosen_sign_is_bigger() {
        val images = barrierImages(images(words = emptyList()))

        val chosen = Severity.entries.map { images.getValue(chosenBarrierImage(it)) }
        assertEquals(listOf(CHOSEN_BARRIER_DP * 2), chosen.map { it.width }.distinct())
        assertTrue(chosen.first().width > images.getValue(barrierSignImage(Severity.entries.first())).width)
        assertFalse(chosen.first().sameAs(chosen.last()))
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
