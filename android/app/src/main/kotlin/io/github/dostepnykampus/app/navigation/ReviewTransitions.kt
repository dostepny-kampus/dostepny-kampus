/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.compose.runtime.Composable
import io.github.dostepnykampus.feature.review.navigation.ReviewQueueRoute
import io.github.dostepnykampus.feature.review.queue.ReviewQueueScreen

/** L2 → R1: „Zgłoszenia do sprawdzenia”. Wymagania: US10.4 */
fun AppNavigator.reviewQueue() {
    leaving { add(ReviewQueueRoute) }
}

/**
 * R1 „Zgłoszenia do sprawdzenia”: „Wstecz” wraca do L2.
 *
 * TODO(#3761): wzięte zgłoszenie i „Otwórz” prowadzą do R2.
 *
 * Wymagania: US10.4, US10.7
 */
@Composable
internal fun AppNavigator.ReviewQueue() {
    ReviewQueueScreen(onBack = { back() }, onOpen = {})
}
