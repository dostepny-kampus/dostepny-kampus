/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.feature.map.navigation.MapRoute

/** Znacznik zastępczej mapy V1 pod arkuszem karty. */
internal const val MAP_TAG = "V1"

/**
 * Powłoka aplikacji z zastępczą mapą V1 i arkuszem karty budynku A-1 z treścią [card], otwartym dotknięciem „A-1”.
 */
internal fun ComposeContentTestRule.openCardOverMap(card: @Composable AppNavigator.() -> Unit) {
    setContent {
        AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) { route ->
            if (route is MapRoute) {
                Column(modifier = Modifier.fillMaxSize().testTag(MAP_TAG)) {
                    Text(text = "A-1", modifier = Modifier.clickable { openBuilding(symbol = "A-1") })
                }
            } else {
                card()
            }
        }
    }
    onNodeWithText("A-1").performClick()
    waitForIdle()
}
