/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.sync

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.capture.sync.SyncContentTest.Companion.QUEUE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Zrzuty C4 bez konfliktu: kolejka w jasnym i ciemnym motywie, czcionka 200 % oraz brak sieci z ostrzeżeniem
 * o miejscu.
 *
 * Wymagania: US9.1, US9.2, NFR-7, NFR-20
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class SyncScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun capture(phase: SyncPhase, isLowSpace: Boolean = false) {
        val actions = SyncActions(onBack = {}, onSyncNow = {}, onLogin = {})
        val state = SyncUiState.Ready(status = QUEUE, phase = phase, isLowSpace = isLowSpace)
        compose.setContent { DostepnyKampusTheme { SyncContent(state = state, actions = actions) } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage()
    }

    /** Kolejka: liczba, podział, ostatnia synchronizacja i „Synchronizuj teraz”. Wymagania: US9.1 */
    @Test
    fun drawsLightQueue() {
        capture(SyncPhase.Waiting)
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkQueue() {
        capture(SyncPhase.Waiting)
    }

    /** Czcionka 200 %: treść przewija się, czynność zostaje osiągalna. Wymagania: NFR-20 */
    @Test
    fun drawsQueueAtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        capture(SyncPhase.Waiting)
    }

    /** Brak sieci z ostrzeżeniem o miejscu. Wymagania: US9.2, NFR-7 */
    @Test
    fun drawsLightOfflineLowSpace() {
        capture(SyncPhase.Offline, isLowSpace = true)
    }
}
