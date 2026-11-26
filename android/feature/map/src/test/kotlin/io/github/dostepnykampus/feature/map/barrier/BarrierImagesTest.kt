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
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.style.BARRIER_NO_FLOOR_DP
import io.github.dostepnykampus.feature.map.style.BARRIER_SIGN_DP
import io.github.dostepnykampus.feature.map.style.CHOSEN_BARRIER_DP
import io.github.dostepnykampus.feature.map.style.MapImages
import io.github.dostepnykampus.feature.map.style.barrierNoFloorImage
import io.github.dostepnykampus.feature.map.style.barrierSignImage
import io.github.dostepnykampus.feature.map.style.barrierWordImage
import io.github.dostepnykampus.feature.map.style.chosenBarrierImage
import io.github.dostepnykampus.feature.map.style.chosenNoFloorImage
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
    fun drawsSignAndWord() {
        val images = barrierImages(images(words = listOf("Utrudnienie", "Tylko z pomocą", "Blokada")))

        val names = signNames() + Severity.entries.map(::barrierWordImage)
        assertEquals("obrazy znaków i słów", names.toSet(), images.keys)
        val signs = Severity.entries.map { images.getValue(barrierSignImage(it)) }
        assertEquals(listOf(SIGN_PX), signs.map { it.width }.distinct())
        assertFalse(signs.first().sameAs(signs.last()))
    }

    @Test
    fun drawsSignsWithoutWords() {
        val images = barrierImages(images(words = emptyList()))

        assertEquals("bez słów same znaki", signNames().toSet(), images.keys)
    }

    /**
     * Wiersz: V1 z planem, znak bariery bez piętra ma dopisek „?”, więc jest innym obrazem niż zwykły i wybrany.
     *
     * Wymagania: US16.1, BR-14, NFR-21
     */
    @Test
    fun marksUnknownFloorSign() {
        val images = barrierImages(images(words = emptyList()))

        Severity.entries.forEach { severity ->
            val unknown = images.getValue(barrierNoFloorImage(severity))
            assertEquals("bok znaku z „?”", BARRIER_NO_FLOOR_DP * 2, unknown.width)
            assertFalse(
                "wybrany znak z „?” różni się od wybranego bez „?”",
                images.getValue(chosenNoFloorImage(severity)).sameAs(images.getValue(chosenBarrierImage(severity))),
            )
        }
    }

    /**
     * Wiersz: V1, znak wybranej bariery jest osobnym, większym obrazem dla każdej uciążliwości.
     *
     * Wymagania: US1.5, NFR-21
     */
    @Test
    fun enlargesChosenSign() {
        val images = barrierImages(images(words = emptyList()))

        val chosen = Severity.entries.map { images.getValue(chosenBarrierImage(it)) }
        assertEquals(listOf(CHOSEN_BARRIER_DP * 2), chosen.map { it.width }.distinct())
        assertTrue(chosen.first().width > images.getValue(barrierSignImage(Severity.entries.first())).width)
        assertFalse(chosen.first().sameAs(chosen.last()))
    }

    private fun signNames(): List<String> =
        Severity.entries.flatMap {
            listOf(barrierSignImage(it), barrierNoFloorImage(it), chosenBarrierImage(it), chosenNoFloorImage(it))
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
