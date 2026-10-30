/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.help.navigation

import androidx.navigation3.runtime.NavKey

/** V5 Pomoc; [isFirstLaunch]: instrukcja otworzyła się sama przy pierwszym uruchomieniu (US5.1). */
data class HelpRoute(val isFirstLaunch: Boolean = false) : NavKey
