/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import android.content.Context
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.test.FakeImageLoaderEngine
import kotlinx.coroutines.awaitCancellation
import java.io.IOException

internal const val PHOTO_URL = "https://serwer.test/submissions/z1/photos/p1"

private const val PHOTO_COLOR = 0xFF336699.toInt()

/** Ustawia domyślny [ImageLoader] testu na [interceptors] bez sieci. */
@OptIn(DelicateCoilApi::class)
internal fun usePhotoLoader(context: Context, vararg interceptors: Interceptor) {
    val loader = ImageLoader.Builder(context).components { interceptors.forEach { add(it) } }.build()
    SingletonImageLoader.setUnsafe(loader)
}

/** Odpowiedź z gotowym obrazem dla [PHOTO_URL]. */
internal fun photoAvailable(): Interceptor =
    FakeImageLoaderEngine.Builder().intercept(PHOTO_URL, ColorImage(PHOTO_COLOR, width = 64, height = 48)).build()

/** Odpowiedź z błędem pobrania każdego obrazu. */
internal fun photoFailing(): Interceptor =
    object : Interceptor {
        override suspend fun intercept(chain: Interceptor.Chain): ImageResult =
            ErrorResult(image = null, request = chain.request, throwable = IOException("brak zdjęcia"))
    }

/** Pobranie, które nie kończy się nigdy. */
internal fun photoPending(): Interceptor =
    object : Interceptor {
        override suspend fun intercept(chain: Interceptor.Chain): ImageResult = awaitCancellation()
    }
