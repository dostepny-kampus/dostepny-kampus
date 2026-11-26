/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Odstepy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Zrzuty panelu informacyjnego i dwóch kart pozycji listy z czynnościami na tle ekranu `surfaceContainer`: jasny,
 * ciemny i czcionka 200 %.
 *
 * Wymagania: NFR-16, NFR-20, NFR-21
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h900dp-xhdpi")
class CardScreenshotTest {
    @Test
    fun drawsLightCards() {
        capture()
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkCards() {
        capture()
    }

    @Test
    fun drawsCardsAtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        capture()
    }

    private fun capture() {
        captureRoboImage {
            DostepnyKampusTheme {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(Odstepy.m),
                    verticalArrangement = Arrangement.spacedBy(Odstepy.m),
                ) {
                    InfoPanel {
                        Text(text = "Twoje role: Odwiedzający, Zbierający", style = MaterialTheme.typography.bodyLarge)
                    }
                    ListItemCard {
                        Text(text = "Próg", style = MaterialTheme.typography.titleMedium)
                        Text(text = "Budynek C-13")
                        CardAction(text = "Otwórz", onClick = {}, opensScreen = true)
                    }
                    ListItemCard {
                        Text(text = "Parter", style = MaterialTheme.typography.titleMedium)
                        Text(text = "Nieobejrzane")
                        CardAction(text = "Oznacz jako obejrzane", onClick = {})
                    }
                }
            }
        }
    }
}
