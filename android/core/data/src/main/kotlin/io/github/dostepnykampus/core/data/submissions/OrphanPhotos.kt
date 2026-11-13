/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.PendingSubmissionDao
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.hours

private val UNTOUCHED_FOR_MILLIS = 1.hours.inWholeMilliseconds

/**
 * Usuwa z [dir] pliki zdjęć bez wiersza w `pending_photos` i spoza [kept], np. zdjęcia wysłanych zgłoszeń albo
 * z porzuconych szkiców po zabiciu procesu. Zdjęcia zgłoszeń czekających i odrzuconych zostają.
 * Plik zmieniony w ostatniej godzinie też zostaje: może go jeszcze zapisywać aparat albo przekodowanie przed
 * zapisem szkicu.
 *
 * Wymagania: NFR-5
 *
 * @param store kolejka zgłoszeń w bazie telefonu
 * @param dir katalog zdjęć zgłoszeń
 * @param kept pliki odtworzonych szkiców: C2 i zgłoszenia szybkiego
 * @param nowMillis chwila sprzątania
 * @param ioContext kontekst operacji na plikach
 */
internal suspend fun deleteOrphanPhotos(
    store: PendingSubmissionDao,
    dir: File,
    kept: Set<String>,
    nowMillis: Long,
    ioContext: CoroutineContext,
) {
    val used = (store.allPhotoPaths() + kept).map { File(it).absolutePath }.toSet()
    withContext(ioContext) {
        dir
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.absolutePath !in used && nowMillis - it.lastModified() >= UNTOUCHED_FOR_MILLIS }
            .forEach { it.delete() }
    }
}
