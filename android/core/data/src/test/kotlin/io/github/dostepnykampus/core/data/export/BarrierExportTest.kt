/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.export

import io.github.dostepnykampus.core.model.export.ExportFormat
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.export.ExportFile
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.LocalDate
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Pobranie eksportu i zapis w „Pobrane” jako wynik dla ekranu K3.
 *
 * Wymagania: US13, US13.1, US13.2
 */
class BarrierExportTest {
    private val saved = mutableListOf<List<String>>()

    @Test
    fun savesFileWithDatedName() {
        val outcome = download(ApiResult.Success(ExportFile("a\nb\n", isEmpty = false)), ExportFormat.CSV)
        assertEquals(ExportOutcome.Saved(isEmpty = false), outcome, "plik zapisany")
        val expected = listOf("dostepny-kampus-bariery-2026-11-25.csv", "text/csv", "a\nb\n")
        assertEquals(listOf(expected), saved, "nazwa, typ i treść pliku")
    }

    @Test
    fun keepsEmptyFile() {
        val outcome = download(ApiResult.Success(ExportFile("{}", isEmpty = true)), ExportFormat.GEOJSON)
        assertEquals(ExportOutcome.Saved(isEmpty = true), outcome, "pusty plik też się zapisuje")
        assertEquals("dostepny-kampus-bariery-2026-11-25.geojson", saved.single().first(), "nazwa pliku GeoJSON")
    }

    @Test
    fun noNetworkIsOffline() {
        val outcome = download(ApiResult.NetworkError(IOException("brak sieci")), ExportFormat.CSV)
        assertEquals(ExportOutcome.Offline, outcome, "brak sieci")
        assertEquals(emptyList<List<String>>(), saved, "nic nie zapisane")
    }

    @Test
    fun refusalsAreSeparate() {
        assertEquals(ExportOutcome.Forbidden, download(ApiResult.ServerError(403), ExportFormat.CSV), "403")
        assertEquals(ExportOutcome.Failed, download(ApiResult.ServerError(500), ExportFormat.CSV), "500")
    }

    @Test
    fun writeErrorIsFailed() {
        val outcome = download(ApiResult.Success(ExportFile("a", isEmpty = false)), ExportFormat.CSV, canWrite = false)
        assertEquals(ExportOutcome.Failed, outcome, "zapis się nie udał")
    }

    private fun download(reply: ApiResult<ExportFile>, format: ExportFormat, canWrite: Boolean = true): ExportOutcome {
        val folder =
            DownloadsFolder { name, mediaType, text ->
                if (canWrite) saved += listOf(name, mediaType, text)
                canWrite
            }
        val export =
            barrierExportOf(
                fetch = { reply },
                folder = folder,
                today = { LocalDate.parse("2026-11-25") },
                work = EmptyCoroutineContext,
            )
        return runBlocking { export.download(format) }
    }
}
