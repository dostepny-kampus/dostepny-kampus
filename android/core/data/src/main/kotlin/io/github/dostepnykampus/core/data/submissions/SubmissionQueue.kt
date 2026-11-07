/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.PendingPhotoEntity
import io.github.dostepnykampus.core.database.PendingSubmissionDao
import io.github.dostepnykampus.core.database.PendingSubmissionEntity
import io.github.dostepnykampus.core.database.PendingSubmissionWithPhotos
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.SubmissionEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * Zdjęcie do wysłania po treści zgłoszenia.
 *
 * @property photoId identyfikator UUID zdjęcia nadany w telefonie (BR-7)
 * @property path ścieżka pliku JPEG w pamięci aplikacji
 */
data class PendingPhotoDraft(val photoId: String, val path: String)

/**
 * Zgłoszenie Zbierającego poprawiane z listy „Moje zgłoszenia” (US8.3).
 *
 * @property id identyfikator poprawianego zgłoszenia; zapis go nie zmienia, więc nie powstaje duplikat
 * @property baseVersion wersja z serwera, od której zaczęto poprawkę; `null`, gdy zgłoszenie jest tylko w telefonie
 */
data class EditedSubmission(val id: String, val baseVersion: Int?)

/** Wiersz zdjęcia z projektu do zapisania w bazie. */
internal fun PendingPhotoDraft.toPendingPhotoEntity(submissionId: String): PendingPhotoEntity =
    PendingPhotoEntity(
        submissionId = submissionId,
        photoId = photoId,
        path = path,
        isUploaded = false,
    )

/**
 * SENT: wysłane; LATER: brak sieci albo awaria serwera (5xx), spróbować później (BR-9);
 * LOGIN_EXPIRED: 401, czekać na ponowne zalogowanie (BR-26); REFUSED: inna odmowa serwera.
 */
internal enum class Outcome { SENT, LATER, LOGIN_EXPIRED, REFUSED }

private const val HTTP_UNAUTHORIZED = 401
private const val FIRST_SERVER_FAULT_CODE = 500

/**
 * Wywołania serwera zgłoszeń, z których korzysta kolejka (`SubmissionsApi`).
 *
 * @property create nowe zgłoszenie (`POST /submissions`)
 * @property putPhoto zdjęcie zgłoszenia (`PUT /submissions/{id}/photos/{photoId}`)
 * @property update edycja z wersją bazową (`PUT /submissions/{id}`, BR-10)
 */
data class SubmissionServer(
    val create: suspend (id: String, content: SubmissionContent) -> ApiResult<Submission>,
    val putPhoto: suspend (submissionId: String, photoId: String, jpeg: ByteArray) -> ApiResult<Unit>,
    val update: suspend (id: String, version: Int, content: SubmissionContent) -> ApiResult<SubmissionEdit>,
)

/**
 * Kolejka zgłoszeń: zapis w telefonie i wysyłanie od najstarszego (NFR-5, US9.1, US9.2, BR-9).
 *
 * Bez sieci albo po błędzie serwera zgłoszenie zostaje w kolejce: zapis w bazie mówi, co serwer
 * już ma, więc kolejna próba wysyła treść raz i tylko brakujące zdjęcia (NFR-8).
 * Identyfikator powstaje raz, więc serwer nie dostanie drugiego zgłoszenia (BR-9).
 * Po zapisie [onSaved] planuje wysyłkę po powrocie sieci (NFR-6); zapis działa także bez niego.
 * Poprawka z C3 ([saveEdit]) zastępuje wiersz tego samego zgłoszenia, a do serwera idzie jako edycja
 * [SubmissionServer.update] z wersją bazową (US8.3, BR-10).
 * Po 401 wysyłka czeka na ponowne zalogowanie: [loginExpired] i [onLoggedIn] (BR-26, US14.4).
 */
class SubmissionQueue(
    private val store: PendingSubmissionDao,
    private val ioContext: CoroutineContext = Dispatchers.IO,
    private val server: SubmissionServer,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val onSaved: () -> Unit = {},
) {
    private val lock = Mutex()
    private val expired = MutableStateFlow(false)

    /** `true` po odpowiedzi 401 aż do [onLoggedIn]: aplikacja prosi o ponowne zalogowanie (BR-26). */
    val loginExpired: StateFlow<Boolean> = expired.asStateFlow()

    /** Kolejka bez edycji na serwerze: poprawka z wersją bazową zostaje w telefonie jak odmowa (404). */
    constructor(
        store: PendingSubmissionDao,
        ioContext: CoroutineContext = Dispatchers.IO,
        create: suspend (id: String, content: SubmissionContent) -> ApiResult<Submission>,
        putPhoto: suspend (submissionId: String, photoId: String, jpeg: ByteArray) -> ApiResult<Unit>,
        newId: () -> String = { UUID.randomUUID().toString() },
        onSaved: () -> Unit = {},
    ) : this(
        store = store,
        ioContext = ioContext,
        server = SubmissionServer(create = create, putPhoto = putPhoto, update = { _, _, _ -> ApiResult.NotFound }),
        newId = newId,
        onSaved = onSaved,
    )

    /**
     * Zapisuje zgłoszenie w kolejce z nowym identyfikatorem w telefonie (BR-9).
     *
     * @param content treść zgłoszenia
     * @param photos zdjęcia do wysłania; identyfikatory wracają w [content] (BR-7, BR-8)
     */
    suspend fun save(content: SubmissionContent, photos: List<PendingPhotoDraft>): PendingSubmissionEntity {
        require(photos.map { it.photoId } == content.photoIds) {
            "Zdjęcia w kolejce muszą odpowiadać identyfikatorom treści (BR-7, BR-8)"
        }
        val id = newId()
        val entity = content.toPendingSubmissionEntity(id = id, savedAtMillis = System.currentTimeMillis())
        store.save(entity, photos.map { it.toPendingPhotoEntity(submissionId = id) })
        // Awaria planowania nie może zgubić zapisanego zgłoszenia (NFR-5).
        runCatching(onSaved)
        return entity
    }

    /**
     * Zapisuje poprawkę zgłoszenia z C3 pod jego identyfikatorem, więc kolejka nie ma dwóch wersji (US8.3, BR-9).
     *
     * @param edited poprawiane zgłoszenie
     * @param content nowa treść zgłoszenia
     * @param photos zdjęcia do wysłania; identyfikatory wracają w [content] (BR-7, BR-8)
     */
    suspend fun saveEdit(edited: EditedSubmission, content: SubmissionContent, photos: List<PendingPhotoDraft>) {
        require(photos.map { it.photoId } == content.photoIds) {
            "Zdjęcia w kolejce muszą odpowiadać identyfikatorom treści (BR-7, BR-8)"
        }
        val entity =
            content
                .toPendingSubmissionEntity(id = edited.id, savedAtMillis = System.currentTimeMillis())
                .copy(baseVersion = edited.baseVersion)
        lock.withLock { store.save(entity, photos.map { it.toPendingPhotoEntity(submissionId = edited.id) }) }
        runCatching(onSaved)
    }

    /**
     * Wysyła zgłoszenia z kolejki od najstarszych: treść raz, potem tylko nie wysłane zdjęcia.
     * Bez sieci albo przy awarii serwera (5xx) wysyłanie się kończy do kolejnej próby (BR-9). Po 401 kończy się
     * do ponownego zalogowania, bo bez niego serwer odrzuci każde zgłoszenie (BR-26). Inna odmowa serwera (4xx)
     * zostawia zgłoszenie w kolejce i przechodzi do następnego (NFR-5, US9.2).
     *
     * @return `false`, gdy trzeba spróbować później; `true`, gdy wysyłka doszła do końca kolejki albo czeka
     *   na zalogowanie, a nie na czas
     */
    suspend fun sendAll(): Boolean =
        lock.withLock {
            val stop = if (expired.value) Outcome.LOGIN_EXPIRED else sendUntilStop()
            if (stop == Outcome.LOGIN_EXPIRED) expired.value = true
            stop != Outcome.LATER
        }

    /** Po ponownym zalogowaniu wysyłka rusza sama, bez działania Zbierającego (BR-26, US14.4). */
    fun onLoggedIn() {
        expired.value = false
        runCatching(onSaved)
    }

    private suspend fun sendUntilStop(): Outcome? =
        store.all().firstNotNullOfOrNull { item ->
            sendOne(item).takeIf { it == Outcome.LATER || it == Outcome.LOGIN_EXPIRED }
        }

    private suspend fun sendOne(item: PendingSubmissionWithPhotos): Outcome =
        if (item.submission.isCreatedOnServer) {
            sendRemaining(item)
        } else {
            val status = sendContent(item)
            if (status == Outcome.SENT) sendRemaining(item) else status
        }

    private suspend fun sendContent(item: PendingSubmissionWithPhotos): Outcome {
        val version = item.submission.baseVersion
        val content = item.toSubmissionContent()
        val outcome =
            if (version == null) {
                outcomeOf(server.create(item.submission.id, content))
            } else {
                editOutcome(server.update(item.submission.id, version, content))
            }
        if (outcome == Outcome.SENT) {
            store.markCreated(id = item.submission.id)
        }
        return outcome
    }

    private suspend fun sendRemaining(item: PendingSubmissionWithPhotos): Outcome {
        for (photo in item.photos.filterNot { it.isUploaded }) {
            val result = server.putPhoto(item.submission.id, photo.photoId, jpeg(photo.path))
            if (result !is ApiResult.Success) {
                return outcomeOf(result)
            }
            store.markUploaded(submissionId = item.submission.id, photoId = photo.photoId)
        }
        store.delete(item.submission.id)
        return Outcome.SENT
    }

    private suspend fun jpeg(path: String): ByteArray = withContext(ioContext) { File(path).readBytes() }
}

internal fun outcomeOf(result: ApiResult<*>): Outcome =
    when (result) {
        is ApiResult.NetworkError -> Outcome.LATER
        is ApiResult.ServerError -> serverOutcome(result.code)
        ApiResult.NotFound -> Outcome.REFUSED
        is ApiResult.Success -> Outcome.SENT
    }

// Konflikt wersji czeka w telefonie na wybór Zbierającego, bez ponawiania w kółko (US9.3, BR-10).
internal fun editOutcome(result: ApiResult<SubmissionEdit>): Outcome =
    if (result is ApiResult.Success && result.value is SubmissionEdit.Conflict) {
        Outcome.REFUSED
    } else {
        outcomeOf(result)
    }

internal fun serverOutcome(code: Int): Outcome =
    when {
        code >= FIRST_SERVER_FAULT_CODE -> Outcome.LATER
        code == HTTP_UNAUTHORIZED -> Outcome.LOGIN_EXPIRED
        else -> Outcome.REFUSED
    }
