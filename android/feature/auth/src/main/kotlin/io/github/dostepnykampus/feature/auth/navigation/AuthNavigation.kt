/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.auth.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * L1 Prośba o logowanie (US14.2). [isForReport]: wejście ze „Zgłoś barierę”, więc po zalogowaniu otworzy się V4;
 * inaczej wejście z „Konto” i potem L2 (UC7 1a, P3).
 */
@Serializable
data class SignInRoute(val isForReport: Boolean) : NavKey

/** L2 Konto (US14.1). */
@Serializable
data object AccountRoute : NavKey
