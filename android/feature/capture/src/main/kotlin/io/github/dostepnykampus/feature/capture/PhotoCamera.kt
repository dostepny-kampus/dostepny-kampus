/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.CoroutineContext

private const val PHOTO_AUTHORITY_SUFFIX = ".zdjecia"

/** Plik zdjęcia [id] w katalogu zdjęć zgłoszeń w [filesDir]; katalog powstaje przy pierwszym zdjęciu. */
internal fun photoFile(filesDir: File, id: String): File =
    File(filesDir, PHOTO_DIR).apply { mkdirs() }.resolve("$id.jpg")

/**
 * „Zrób zdjęcie” na C2: aplikacja aparatu zapisuje plik w pamięci aplikacji, a po zrobieniu zdjęcia plik jest
 * przekodowany poza wątkiem interfejsu i trafia do [onPhoto] (US7.1, NFR-26). Rezygnacja w aparacie i telefon bez
 * aparatu zostawiają formularz bez zmian.
 * Dostawca plików dla aparatu ma nazwę zgodną z `AndroidManifest.xml` modułu.
 *
 * @param onPhoto zdjęcie gotowe do formularza
 * @param workContext wątek przekodowania pliku
 * @return czynność „Zrób zdjęcie”
 */
@Composable
internal fun rememberPhotoCamera(
    onPhoto: (PendingPhotoDraft) -> Unit,
    workContext: CoroutineContext = Dispatchers.IO,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deliver by rememberUpdatedState(onPhoto)
    var pendingId by rememberSaveable { mutableStateOf("") }
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { isTaken ->
            val id = pendingId
            pendingId = ""
            if (id.isNotEmpty()) {
                val file = photoFile(filesDir = context.filesDir, id = id)
                scope.launch {
                    val isReady = isTaken && withContext(workContext) { shrinkPhoto(file) }
                    if (isReady) deliver(PendingPhotoDraft(photoId = id, path = file.path)) else file.delete()
                }
            }
        }
    return {
        pendingId = UUID.randomUUID().toString()
        val file = photoFile(filesDir = context.filesDir, id = pendingId)
        try {
            camera.launch(FileProvider.getUriForFile(context, context.packageName + PHOTO_AUTHORITY_SUFFIX, file))
        } catch (_: ActivityNotFoundException) {
            pendingId = ""
        }
    }
}
