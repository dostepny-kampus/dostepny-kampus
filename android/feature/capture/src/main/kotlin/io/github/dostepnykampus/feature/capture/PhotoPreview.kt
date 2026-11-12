/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
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
 * Wątek czytania plików podglądu zdjęć, poza wątkiem interfejsu. Testy podają kontekst testowy, więc
 * `waitForIdle` obejmuje czytanie pliku bez czekania oknem zegara (#622). W aplikacji to `Dispatchers.IO`,
 * podany parametrem domyślnym jak w [PhotoTranscoder].
 */
internal val LocalPhotoDecodeContext = staticCompositionLocalOf { photoDecodeContext() }

private fun photoDecodeContext(context: CoroutineContext = Dispatchers.IO): CoroutineContext = context

/**
 * Podgląd zrobionych zdjęć nad „Zrób nowe zdjęcie”, jak w hi-fi C2 w stanie edycji (US7.1). Pliki czyta
 * w [LocalPhotoDecodeContext].
 *
 * @param photos zdjęcia formularza
 */
@Composable
internal fun PhotoPreviews(photos: PhotoPaths) {
    val decodeContext = LocalPhotoDecodeContext.current
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
internal fun decodePreview(path: String): ImageBitmap? =
    decodeSampled(path = path, limit = PREVIEW_PIXELS)?.asImageBitmap()
