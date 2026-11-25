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

/** Wynik pobrania eksportu. Wymagania: US13 */
public sealed interface ExportOutcome {
    /**
     * Plik zapisany w wybranym miejscu.
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

/** Miejsce zapisu pliku eksportu. Wymagania: US13, NFR-41 */
public sealed interface ExportPlace {
    /** Folder „Pobrane” telefonu; tylko gdy [BarrierExport.savesToDownloads]. */
    public data object Downloads : ExportPlace

    /**
     * Plik wybrany przez osobę w systemowym oknie zapisu.
     *
     * @property uri adres dokumentu zwrócony przez okno systemu
     */
    public data class Chosen(val uri: String) : ExportPlace
}

/** Eksport bieżących zgłoszeń przyjętych do pliku; wymaga sieci. Wymagania: US13, US13.1, US13.2, NFR-41 */
public interface BarrierExport {
    /** `true`, gdy telefon zapisuje plik w „Pobrane” bez pytania; inaczej miejsce wybiera osoba w oknie systemu. */
    public val savesToDownloads: Boolean

    /** Nazwa pliku eksportu w formacie [format] z dzisiejszą datą. */
    public fun fileName(format: ExportFormat): String

    /** Pobiera plik w formacie [format] i zapisuje go w [place]. */
    public suspend fun download(format: ExportFormat, place: ExportPlace): ExportOutcome
}

/**
 * Miejsca zapisu eksportu.
 *
 * @property downloads folder „Pobrane”
 * @property chosen plik wybrany w oknie systemu
 * @property savesToDownloads czy telefon zapisuje w „Pobrane” bez pytania o miejsce
 */
internal data class ExportPlaces(
    val downloads: DownloadsFolder,
    val chosen: ChosenFile,
    val savesToDownloads: Boolean,
)

/** Eksport z pobraniem [fetch] i zapisem w [places] pod nazwą z datą [today]; zapis idzie w [work]. */
internal class ServerExport(
    private val fetch: suspend (ExportFormat) -> ApiResult<ExportFile>,
    private val places: ExportPlaces,
    private val today: () -> LocalDate,
    private val work: CoroutineContext,
) : BarrierExport {
    override val savesToDownloads: Boolean get() = places.savesToDownloads

    override fun fileName(format: ExportFormat): String = "dostepny-kampus-bariery-${today()}.${format.extension}"

    override suspend fun download(format: ExportFormat, place: ExportPlace): ExportOutcome =
        when (val reply = fetch(format)) {
            is ApiResult.Success -> save(format, reply.value, place)
            is ApiResult.NetworkError -> ExportOutcome.Offline
            is ApiResult.ServerError -> refused(reply.code)
            ApiResult.NotFound -> ExportOutcome.Failed
        }

    private suspend fun save(format: ExportFormat, file: ExportFile, place: ExportPlace): ExportOutcome {
        val isSaved = withContext(work) { write(format, file, place) }
        return if (isSaved) ExportOutcome.Saved(isEmpty = file.isEmpty) else ExportOutcome.Failed
    }

    private fun write(format: ExportFormat, file: ExportFile, place: ExportPlace): Boolean =
        when (place) {
            ExportPlace.Downloads -> {
                places.downloads.save(name = fileName(format), mediaType = format.mediaType, text = file.text)
            }

            is ExportPlace.Chosen -> {
                places.chosen.save(uri = place.uri, text = file.text)
            }
        }

    private fun refused(code: Int): ExportOutcome =
        if (code == HTTP_FORBIDDEN) ExportOutcome.Forbidden else ExportOutcome.Failed
}
