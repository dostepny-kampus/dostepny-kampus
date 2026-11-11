/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Zrzuty wspólnego wyboru piętra: chipy (C2) i lista (V4), jasny, ciemny i czcionka 200 % (NFR-16, NFR-20). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h900dp-xhdpi")
class FloorPickerScreenshotTest {
    @Test
    fun nfr16_pietra_jasny() {
        capture()
    }

    @Test
    fun nfr16_pietra_ciemny() {
        RuntimeEnvironment.setQualifiers("+night")
        capture()
    }

    @Test
    fun nfr20_pietra_200() {
        RuntimeEnvironment.setFontScale(2f)
        capture()
    }

    private fun capture() {
        val labels = listOf("Nie wiem", "Piwnica", "Parter", "1 piętro", "2 piętro")
        val options = FloorOptions(labels.map { FloorOption(key = it, label = it) })
        captureRoboImage {
            DostepnyKampusTheme {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(Odstepy.m),
                    verticalArrangement = Arrangement.spacedBy(Odstepy.l),
                ) {
                    FloorPicker(
                        options = options,
                        selectedKey = "Parter",
                        onSelect = {},
                        layout = FloorPickerLayout.CHIPS,
                    )
                    FloorPicker(options = options, selectedKey = null, onSelect = {}, layout = FloorPickerLayout.LIST)
                }
            }
        }
    }
}
