/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** C4 bez konfliktu: kolejka wysyłki i „Synchronizuj teraz”. Wymagania: US9.1, US9.2 */
@Serializable
data object SyncRoute : NavKey
