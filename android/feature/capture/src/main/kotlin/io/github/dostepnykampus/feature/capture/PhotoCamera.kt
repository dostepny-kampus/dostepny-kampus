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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import java.io.File
import java.util.UUID

private const val PHOTO_AUTHORITY_SUFFIX = ".zdjecia"

/** Katalog zdjęć zgłoszeń w [filesDir], także dla sprzątania przy starcie (#927). */
fun photoDir(filesDir: File): File = File(filesDir, PHOTO_DIR)

/** Plik zdjęcia [id] w katalogu zdjęć zgłoszeń w [filesDir]; katalog powstaje przy pierwszym zdjęciu. */
internal fun photoFile(filesDir: File, id: String): File = photoDir(filesDir).apply { mkdirs() }.resolve("$id.jpg")

/**
 * „Zrób zdjęcie” z wynikiem ostatniej próby.
 *
 * @property onTake otwiera aparat
 * @property isFailed zdjęcia nie dało się przekodować; komunikat znika przy następnej próbie
 */
@Immutable
data class PhotoCamera(val onTake: () -> Unit, val isFailed: Boolean = false)

/**
 * „Zrób zdjęcie” na C2: aplikacja aparatu zapisuje plik w pamięci aplikacji, a po zrobieniu zdjęcia plik jest
 * przekodowany poza wątkiem interfejsu i trafia do [onPhoto] (US7.1, NFR-26). Rezygnacja w aparacie i telefon bez
 * aparatu zostawiają formularz bez zmian. Błąd przekodowania usuwa plik i ustawia [PhotoCamera.isFailed], a aplikacja
 * działa dalej (NFR-50). Przekodowanie trwa w [PhotoTranscoder], więc obrót ekranu nie gubi zdjęcia (NFR-22).
 * Dostawca plików dla aparatu ma nazwę zgodną z `AndroidManifest.xml` modułu.
 *
 * @param onPhoto zdjęcie gotowe do formularza
 * @param transcoder przekodowanie zdjęcia, które przetrwa obrót ekranu
 * @return czynność „Zrób zdjęcie” i wynik ostatniej próby
 */
@Composable
internal fun rememberPhotoCamera(onPhoto: (PendingPhotoDraft) -> Unit, transcoder: PhotoTranscoder): PhotoCamera {
    val context = LocalContext.current
    val deliver by rememberUpdatedState(onPhoto)
    val ready by transcoder.ready.collectAsStateWithLifecycle()
    val isFailed by transcoder.failure.collectAsStateWithLifecycle()
    val delivery = ready
    SideEffect {
        delivery?.let { photo ->
            deliver(photo)
            transcoder.delivered()
        }
    }
    var pendingId by rememberSaveable { mutableStateOf("") }
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { isTaken ->
            val id = pendingId
            pendingId = ""
            if (id.isNotEmpty()) {
                val file = photoFile(filesDir = context.filesDir, id = id)
                if (isTaken) transcoder.start(id = id, file = file) else file.delete()
            }
        }
    return PhotoCamera(
        onTake = {
            transcoder.retry()
            pendingId = UUID.randomUUID().toString()
            val file = photoFile(filesDir = context.filesDir, id = pendingId)
            try {
                camera.launch(FileProvider.getUriForFile(context, context.packageName + PHOTO_AUTHORITY_SUFFIX, file))
            } catch (_: ActivityNotFoundException) {
                pendingId = ""
            }
        },
        isFailed = isFailed,
    )
}
