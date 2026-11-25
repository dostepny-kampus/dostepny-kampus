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
import io.github.dostepnykampus.feature.review.queue.ReviewQueueScreen

/** L2 → R1: „Zgłoszenia do sprawdzenia”. Wymagania: US10.4 */
fun AppNavigator.reviewQueue() {
    leaving { add(ReviewQueueRoute) }
}

/** R1 → R2 zgłoszenia [submissionId]; poza R1 nic nie robi. Wymagania: US10.7 */
fun AppNavigator.openReviewCheck(submissionId: String) {
    if (current == ReviewQueueRoute) leaving { add(ReviewCheckRoute(submissionId)) }
}

/** R2 → R1 po zapisanej decyzji: R2 znika z historii, a R1 czyta kolejkę od nowa. Wymagania: US10, NFR-54 */
fun AppNavigator.reviewDecided() {
    backToFresh()
}

/**
 * R1 „Zgłoszenia do sprawdzenia” albo R2 „Sprawdzenie zgłoszenia” według [route]. Z R1 „Wstecz” wraca do L2,
 * a wzięte zgłoszenie i „Otwórz” prowadzą do R2; z R2 „Wstecz” wraca do R1, decyzja do R1 ze świeżą kolejką.
 *
 * Wymagania: US10, US10.4, US10.7, NFR-54
 */
@Composable
internal fun AppNavigator.Review(route: NavKey) {
    if (route is ReviewCheckRoute) {
        ReviewCheckScreen(route.submissionId, ReviewCheckActions(onBack = { back() }, onDecided = { reviewDecided() }))
    } else {
        ReviewQueueScreen(onBack = { back() }, onOpen = { openReviewCheck(it) })
    }
}
