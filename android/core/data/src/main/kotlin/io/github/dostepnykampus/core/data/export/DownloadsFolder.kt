/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.export

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import java.io.IOException

/** Folder „Pobrane” telefonu. Wymagania: US13 */
internal fun interface DownloadsFolder {
    /** Zapisuje plik [name] typu [mediaType] z treścią [text] w UTF-8; `true`, gdy zapis się udał. */
    fun save(name: String, mediaType: String, text: String): Boolean
}

/** Plik wybrany przez osobę w systemowym oknie zapisu. Wymagania: US13, NFR-41 */
internal fun interface ChosenFile {
    /** Zapisuje treść [text] w UTF-8 w dokumencie [uri]; `true`, gdy zapis się udał. */
    fun save(uri: String, text: String): Boolean
}

/**
 * „Pobrane” przez MediaStore, bez uprawnień do pamięci od Androida 10; wcześniej zapis się nie udaje.
 *
 * @param resolver dostęp do MediaStore
 */
internal class MediaStoreDownloads(private val resolver: ContentResolver) : DownloadsFolder {
    override fun save(name: String, mediaType: String, text: String): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && write(name, mediaType, text)

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun write(name: String, mediaType: String, text: String): Boolean {
        val values = ContentValues()
        values.put(MediaStore.Downloads.DISPLAY_NAME, name)
        values.put(MediaStore.Downloads.MIME_TYPE, mediaType)
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { stream -> stream.write(text.toByteArray()) } != null
        } catch (_: IOException) {
            resolver.delete(uri, null, null)
            false
        }
    }
}

/**
 * Dokument z okna systemu zapisany przez [resolver]; nowa treść zastępuje poprzednią.
 *
 * @param resolver dostęp do dokumentu
 */
internal class ResolverChosenFile(private val resolver: ContentResolver) : ChosenFile {
    override fun save(uri: String, text: String): Boolean =
        try {
            resolver.openOutputStream(uri.toUri(), "wt")?.use { output -> output.write(text.toByteArray()) } != null
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
}
