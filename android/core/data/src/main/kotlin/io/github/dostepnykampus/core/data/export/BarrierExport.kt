/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.export

import io.github.dostepnykampus.core.model.export.ExportFormat
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.export.ExportFile
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext

/** Wynik pobrania eksportu do folderu „Pobrane”. Wymagania: US13 */
public sealed interface ExportOutcome {
    /**
     * Plik zapisany w „Pobrane”.
     *
     * @property isEmpty czy plik nie ma żadnej bariery
     */
    public data class Saved(val isEmpty: Boolean) : ExportOutcome

    /** Brak połączenia z serwerem. */
    public data object Offline : ExportOutcome

    /** Konto nie ma roli Koordynatora (403). */
    public data object Forbidden : ExportOutcome

    /** Błąd serwera albo zapisu pliku. */
    public data object Failed : ExportOutcome
}

/** Eksport bieżących zgłoszeń przyjętych do pliku w „Pobrane”; wymaga sieci. Wymagania: US13, US13.1, US13.2 */
public fun interface BarrierExport {
    /** Pobiera plik w formacie [format] i zapisuje go w „Pobrane”. */
    public suspend fun download(format: ExportFormat): ExportOutcome
}

/**
 * Eksport z pobraniem [fetch] i zapisem w [folder] pod nazwą z datą [today]; zapis idzie w [work].
 *
 * @param fetch pobranie pliku z serwera
 * @param folder folder „Pobrane”
 * @param today dzisiejsza data do nazwy pliku
 * @param work wątek zapisu
 */
internal fun barrierExportOf(
    fetch: suspend (ExportFormat) -> ApiResult<ExportFile>,
    folder: DownloadsFolder,
    today: () -> LocalDate,
    work: CoroutineContext,
): BarrierExport =
    BarrierExport { format ->
        when (val reply = fetch(format)) {
            is ApiResult.Success -> {
                val name = "dostepny-kampus-bariery-${today()}.${format.extension}"
                val file = reply.value
                val type = format.mediaType
                val isSaved = withContext(work) { folder.save(name = name, mediaType = type, text = file.text) }
                if (isSaved) ExportOutcome.Saved(isEmpty = file.isEmpty) else ExportOutcome.Failed
            }

            is ApiResult.NetworkError -> ExportOutcome.Offline
            is ApiResult.ServerError -> refusal(reply.code)
            ApiResult.NotFound -> ExportOutcome.Failed
        }
    }

private fun refusal(code: Int): ExportOutcome =
    if (code == HTTP_FORBIDDEN) ExportOutcome.Forbidden else ExportOutcome.Failed
