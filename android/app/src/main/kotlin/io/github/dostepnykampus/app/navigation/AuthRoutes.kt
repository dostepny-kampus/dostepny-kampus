/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * L1 Prośba o logowanie. [isForReport]: wejście ze „Zgłoś barierę”, więc po zalogowaniu otworzy się V4
 * z wejściem [entranceId], jeśli było wybrane; inaczej wejście z „Konto” i potem L2 (P3).
 *
 * Wymagania: US14.2, UC7
 */
@Serializable
data class SignInRoute(val isForReport: Boolean, val entranceId: String? = null) : NavKey

/** L2 Konto. Wymagania: US14.1 */
@Serializable
data object AccountRoute : NavKey
