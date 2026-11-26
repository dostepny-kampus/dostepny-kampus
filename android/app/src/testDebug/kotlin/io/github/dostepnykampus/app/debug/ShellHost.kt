/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.debug

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.AppShell
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.AppScreen
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.feature.map.navigation.MapRoute

/**
 * Pokazuje prawdziwy pasek i ekrany aplikacji dla konta [account]; mapa ma lekki zastępnik.
 *
 * @param account konto, z którym osoba jest zalogowana
 */
internal fun ComposeContentTestRule.showShell(account: Account) {
    val roles = TestRoles(account)
    setContent {
        DostepnyKampusTheme {
            AppShell(firstLaunch = false, roles = roles, onFirstHelpLeft = {}) { route -> ShellScreen(route) }
        }
    }
}

@Composable
private fun AppNavigator.ShellScreen(route: NavKey) {
    if (route is MapRoute) Text("V1") else AppScreen(route)
}
