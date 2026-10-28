/*
 * Dostępny Kampus
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 * SPDX-License-Identifier: MIT
 */
package io.github.dostepnykampus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/**
 * Jedyna aktywność aplikacji: po uruchomieniu pokazuje ekran startowy z nazwą aplikacji
 * rysowany od krawędzi do krawędzi, z treścią poza paskami systemowymi.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.safeDrawingPadding(),
            )
        }
    }
}
