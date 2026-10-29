/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h400dp-xhdpi")
class BottomBarScreenshotTest {
    @Test
    fun nfr16_pasek_dolny_jasny() {
        captureRoboImage { Bars() }
    }

    @Test
    fun nfr16_pasek_dolny_ciemny() {
        RuntimeEnvironment.setQualifiers("+night")
        captureRoboImage { Bars() }
    }

    @Test
    fun nfr20_pasek_dolny_czcionka_200() {
        RuntimeEnvironment.setFontScale(2f)
        captureRoboImage { Bars() }
    }

    @Composable
    private fun Bars() {
        DostepnyKampusTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Bar(selected = 2, account = "Konto")
                Bar(selected = 3, account = "Konto i\u00A0praca")
            }
        }
    }

    @Composable
    private fun Bar(selected: Int, account: String) {
        BottomBar {
            listOf(
                "Mapa" to NavigationIcons.Map,
                "Zgłoś barierę" to NavigationIcons.Camera,
                "Pomoc" to NavigationIcons.Help,
                account to NavigationIcons.Account,
            ).forEachIndexed { index, (label, icon) ->
                BottomBarItem(label = label, icon = icon, selected = index == selected, onClick = {})
            }
        }
    }
}
