/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.feature.review.check.ReviewCheckActions
import io.github.dostepnykampus.feature.review.check.ReviewCheckScreen
import io.github.dostepnykampus.feature.review.navigation.ReviewCheckRoute
import io.github.dostepnykampus.feature.review.navigation.ReviewQueueRoute
import io.github.dostepnykampus.feature.review.queue.ReviewQueueActions
import io.github.dostepnykampus.feature.review.queue.ReviewQueueScreen

/** L2 → R1: „Zgłoszenia do sprawdzenia”. Wymagania: US10.4 */
fun AppNavigator.reviewQueue() {
    leaving { add(ReviewQueueRoute) }
}

/** R1 → R2: wzięte zgłoszenie albo „Otwórz” przy zgłoszeniu [submissionId]; działa tylko z R1. Wymagania: US10.7 */
fun AppNavigator.openReviewCheck(submissionId: String) {
    if (current == ReviewQueueRoute) leaving { add(ReviewCheckRoute(submissionId)) }
}

/**
 * R1 „Zgłoszenia do sprawdzenia” i R2 „Sprawdzenie zgłoszenia”: „Wstecz” z R1 wraca do L2, z R2 do R1, a decyzja
 * na R2 wraca do R1 bez R2 w historii.
 *
 * Wymagania: US10, US10.4, US10.7
 */
@Composable
internal fun AppNavigator.Review(route: NavKey) {
    if (route is ReviewCheckRoute) {
        ReviewCheckScreen(route.submissionId, ReviewCheckActions(onBack = { back() }, onDecided = { back() }))
    } else {
        ReviewQueueScreen(ReviewQueueActions(onBack = { back() }, onOpen = { openReviewCheck(it) }))
    }
}
