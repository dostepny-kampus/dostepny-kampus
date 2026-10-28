/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.model.EntranceMarking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h900dp-xhdpi")
class ComponentsScreenshotTest {
    @Test
    fun nfr16_nfr21_motyw_jasny() {
        captureComponents()
    }

    @Test
    fun nfr16_nfr21_motyw_ciemny() {
        RuntimeEnvironment.setQualifiers("+night")
        captureComponents()
    }

    @Test
    fun nfr17_nfr20_czcionka_200() {
        RuntimeEnvironment.setFontScale(2f)
        captureComponents()
    }

    private fun captureComponents() {
        captureRoboImage {
            DostepnyKampusTheme {
                Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) {
                    GroupContainer(title = "Wejścia", modifier = Modifier.padding(horizontal = Odstepy.m)) {
                        Text(text = "Wejście dostosowane", style = MaterialTheme.typography.titleMedium)
                        EntranceMarking.entries.forEach {
                            MarkingChip(marking = it, modifier = Modifier.padding(vertical = Odstepy.xs))
                        }
                    }
                }
            }
        }
    }
}
