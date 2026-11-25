/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.github.dostepnykampus.app.R
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.location.DemoMode
import io.github.dostepnykampus.core.location.TrybPokazu

/**
 * Włącznik trybu pokazu pomiaru C1 wersji debug, z osobną ikoną w launcherze. Stan zapisuje [DemoMode].
 *
 * Wymagania: US6.1
 */
@OptIn(TrybPokazu::class)
class DemoModeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isEnabled by remember { mutableStateOf(DemoMode.isEnabled(this)) }
            DostepnyKampusTheme {
                DemoModeSwitch(
                    isEnabled = isEnabled,
                    onChange = { value ->
                        DemoMode.setEnabled(this, value)
                        isEnabled = value
                    },
                )
            }
        }
    }
}

/**
 * Tytuł, opis i przełącznik trybu pokazu.
 *
 * @param isEnabled czy tryb pokazu jest włączony
 * @param onChange nowy stan przełącznika
 * @param modifier wymiary nadane przez aktywność
 */
@Composable
fun DemoModeSwitch(isEnabled: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(Odstepy.m),
            verticalArrangement = Arrangement.spacedBy(Odstepy.s),
        ) {
            Text(stringResource(R.string.debug_pokaz_tytul), style = MaterialTheme.typography.titleLarge)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = CeleDotyku.przycisk)
                        .toggleable(value = isEnabled, role = Role.Switch, onValueChange = onChange),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
            ) {
                Text(stringResource(R.string.debug_pokaz_opis), modifier = Modifier.weight(1f))
                Switch(checked = isEnabled, onCheckedChange = null)
            }
        }
    }
}
