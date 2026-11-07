/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

private const val PREVIEW_PIXELS = 720
private val PREVIEW_HEIGHT = 200.dp

/**
 * Ścieżki zrobionych zdjęć; opakowanie listy, żeby Compose wiedział, że się nie zmienia.
 *
 * @property items pliki JPEG w pamięci aplikacji, w kolejności zrobienia
 */
@Immutable
data class PhotoPaths(val items: List<String>)

/**
 * Podgląd zrobionych zdjęć nad „Zrób nowe zdjęcie”, jak w hi-fi C2 w stanie edycji (US7.1).
 *
 * @param photos zdjęcia formularza
 * @param decodeContext wątek czytania plików, poza wątkiem interfejsu
 */
@Composable
internal fun PhotoPreviews(photos: PhotoPaths, decodeContext: CoroutineContext = Dispatchers.IO) {
    photos.items.forEachIndexed { index, path ->
        val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
            value = withContext(decodeContext) { decodePreview(path) }
        }
        bitmap?.let { preview ->
            Image(
                bitmap = preview,
                contentDescription = stringResource(R.string.formularz_zdjecie_podglad, index + 1, photos.items.size),
                modifier = Modifier.fillMaxWidth().height(PREVIEW_HEIGHT),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/**
 * Zdjęcie zmniejszone do podglądu, żeby nie czytać do pamięci pełnego JPEG z aparatu.
 *
 * @return obraz albo `null`, gdy pliku nie da się odczytać
 */
internal fun decodePreview(path: String): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    val longerSide = maxOf(a = bounds.outWidth, b = bounds.outHeight)
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(longerSide) }
    return BitmapFactory.decodeFile(path, options)?.asImageBitmap()
}

/** Największa potęga dwójki, po której dłuższy bok ma co najmniej [PREVIEW_PIXELS]. */
internal fun sampleSize(longerSide: Int): Int {
    var sample = 1
    while (longerSide / (sample * 2) >= PREVIEW_PIXELS) sample *= 2
    return sample
}
