/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import coil3.ImageLoader
import dagger.hilt.android.EntryPointAccessors
import io.github.dostepnykampus.app.di.ImageLoaderEntryPoint
import io.github.dostepnykampus.feature.review.check.ReviewCheckActions
import io.github.dostepnykampus.feature.review.check.ReviewCheckScreen
import io.github.dostepnykampus.feature.review.navigation.ReviewCheckRoute
import io.github.dostepnykampus.feature.review.navigation.ReviewCoverRoute
import io.github.dostepnykampus.feature.review.navigation.ReviewQueueRoute
import io.github.dostepnykampus.feature.review.photo.CoverScreen
import io.github.dostepnykampus.feature.review.queue.ReviewQueueScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** L2 → R1: „Zgłoszenia do sprawdzenia”. Wymagania: US10.4 */
fun AppNavigator.reviewQueue() {
    leaving { add(ReviewQueueRoute) }
}

/** R1 → R2 zgłoszenia [submissionId]; poza R1 nic nie robi. Wymagania: US10.7 */
fun AppNavigator.openReviewCheck(submissionId: String) {
    if (current == ReviewQueueRoute) leaving { add(ReviewCheckRoute(submissionId)) }
}

/** Powrót z R2 do R1 po zapisanej decyzji; R2 znika z historii, a R1 czyta kolejkę od nowa. Wymagania: US10, NFR-54 */
fun AppNavigator.reviewDecided() {
    backToFresh()
}

/** R2 → zasłanianie zdjęcia [photoId] spod adresu [photoUrl]; poza R2 nic nie robi. Wymagania: BR-15, NFR-25 */
fun AppNavigator.openCover(submissionId: String, photoId: String, photoUrl: String) {
    if (current == ReviewCheckRoute(submissionId)) {
        leaving { add(ReviewCoverRoute(submissionId = submissionId, photoId = photoId, photoUrl = photoUrl)) }
    }
}

/** Powrót z zasłaniania do R2 po zapisie zdjęcia; R2 czyta zgłoszenie od nowa. Wymagania: BR-15 */
fun AppNavigator.coverSaved() {
    backToFresh()
}

/**
 * R1 „Zgłoszenia do sprawdzenia”, R2 „Sprawdzenie zgłoszenia” albo zasłanianie zdjęcia według [route]. Z R1
 * „Wstecz” wraca do L2, a wzięte zgłoszenie i „Otwórz” prowadzą do R2; z R2 „Wstecz” wraca do R1, a zapisana
 * decyzja też wraca tam ze świeżą kolejką. „Zasłoń twarz lub tablicę” na R2 otwiera zasłanianie; „Wstecz” wraca
 * do R2 bez zmian, a po zapisie R2 czyta zgłoszenie od nowa i wczytuje nowe zdjęcie zamiast obrazu z pamięci.
 *
 * Wymagania: US10, US10.4, US10.7, BR-15, NFR-25, NFR-54
 */
@Composable
internal fun AppNavigator.Review(route: NavKey) {
    when (route) {
        is ReviewCheckRoute -> {
            val actions =
                ReviewCheckActions(
                    onBack = { back() },
                    onDecided = { reviewDecided() },
                    onCover = { photoId, photoUrl ->
                        openCover(submissionId = route.submissionId, photoId = photoId, photoUrl = photoUrl)
                    },
                )
            ReviewCheckScreen(route.submissionId, actions)
        }

        is ReviewCoverRoute -> {
            val images = EntryPointAccessors.fromApplication(LocalContext.current, ImageLoaderEntryPoint::class.java)
            val scope = rememberCoroutineScope()
            CoverScreen(
                submissionId = route.submissionId,
                photoId = route.photoId,
                onBack = { back() },
                onSaveFinish = {
                    scope.launch {
                        images.imageLoader().evictCachedPhoto(
                            url = route.photoUrl,
                            ioContext = images.imageDiskContext(),
                        )
                        if (current == route) coverSaved()
                    }
                },
            )
        }

        else -> {
            ReviewQueueScreen(onBack = { back() }, onOpen = { openReviewCheck(it) })
        }
    }
}

private suspend fun ImageLoader.evictCachedPhoto(url: String, ioContext: CoroutineContext) {
    withContext(ioContext) { diskCache?.remove(url) }
    memoryCache?.run { keys.filter { it.key == url }.forEach { remove(it) } }
}
