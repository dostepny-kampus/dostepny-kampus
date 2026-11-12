/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Gesty mapy V1: północ zostaje u góry, a przesuwanie i powiększanie działają. */
@RunWith(RobolectricTestRunner::class)
class MapGesturesTest {
    private val options = northUpOptions(RuntimeEnvironment.getApplication())

    /** Gest dwoma palcami nie obraca ani nie pochyla mapy, więc strzałki i granica terenu zgadzają się z ekranem. */
    @Test
    fun no_rotate_no_tilt_gesture() {
        assertFalse(options.rotateGesturesEnabled)
        assertFalse(options.tiltGesturesEnabled)
    }

    /** Przesuwanie i powiększanie gestem zostają włączone. */
    @Test
    fun scroll_and_zoom_gestures_stay() {
        assertTrue(options.scrollGesturesEnabled)
        assertTrue(options.zoomGesturesEnabled)
    }
}
