/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.auth.navigation

import androidx.navigation3.runtime.NavKey

/**
 * L1 Prośba o logowanie (US14.2). [isForReport]: wejście ze „Zgłoś barierę”, więc po zalogowaniu otworzy się V4
 * z wejściem [entranceId], jeśli wybrano je na V2 albo V3; inaczej wejście z „Konto” i potem L2 (UC7 1a, P3).
 */
data class SignInRoute(val isForReport: Boolean, val entranceId: String? = null) : NavKey

/** L2 Konto (US14.1). */
data object AccountRoute : NavKey
