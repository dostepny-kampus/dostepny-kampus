/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.OwnSubmissionDao
import io.github.dostepnykampus.core.database.OwnSubmissionState
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
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

/**
 * Zdjęcie do wysłania po treści zgłoszenia.
 *
 * Wymagania: BR-7
 *
 * @property photoId identyfikator UUID zdjęcia nadany w telefonie
 * @property path ścieżka pliku JPEG w pamięci aplikacji
 */
data class PendingPhotoDraft(val photoId: String, val path: String)

/**
 * Zgłoszenie Zbierającego poprawiane z listy „Moje zgłoszenia”.
 *
 * Wymagania: US8.3
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
 * SENT: wysłane; LATER: brak sieci albo awaria serwera (5xx), spróbować później;
 * LOGIN_EXPIRED: 401, czekać na ponowne zalogowanie; REFUSED: inna odmowa serwera;
 * REPLACED: w trakcie wysyłki zapisano poprawkę, więc wynik dotyczy starej treści i wiersz idzie od nowa.
 *
 * Wymagania: BR-9, BR-26
 */
internal enum class Outcome { SENT, LATER, LOGIN_EXPIRED, REFUSED, REPLACED }

private const val HTTP_UNAUTHORIZED = 401
private const val FIRST_SERVER_FAULT_CODE = 500

/**
 * Wywołania serwera zgłoszeń, z których korzysta kolejka (`SubmissionsApi`).
 *
 * Wymagania: BR-10
 *
 * @property create nowe zgłoszenie (`POST /submissions`)
 * @property putPhoto zdjęcie zgłoszenia (`PUT /submissions/{id}/photos/{photoId}`)
 * @property update edycja z wersją bazową (`PUT /submissions/{id}`)
 */
data class SubmissionServer(
    val create: suspend (id: String, content: SubmissionContent) -> ApiResult<Submission>,
    val putPhoto: suspend (submissionId: String, photoId: String, jpeg: ByteArray) -> ApiResult<Unit>,
    val update: suspend (id: String, version: Int, content: SubmissionContent) -> ApiResult<SubmissionEdit>,
)

/**
 * Identyfikator i chwila zapisu nowego zgłoszenia w kolejce; testy podają własne.
 *
 * Wymagania: BR-9
 *
 * @property newId identyfikator zgłoszenia w telefonie, raz na zgłoszenie
 * @property nowMillis zegar chwili zapisu
 */
data class SubmissionStamps(
    val newId: () -> String = { UUID.randomUUID().toString() },
    val nowMillis: () -> Long = System::currentTimeMillis,
)

/**
 * Kolejka zgłoszeń: zapis w telefonie i wysyłanie od najstarszego. Bez sieci albo po błędzie serwera zgłoszenie
 * zostaje, a kolejna próba wysyła treść raz i tylko brakujące zdjęcia, z tym samym identyfikatorem. [onSaved] planuje
 * wysyłkę po powrocie sieci. Poprawka ([saveEdit]) zastępuje wiersz zgłoszenia i idzie jako [SubmissionServer.update] z
 * wersją bazową. Po 401 wysyłka czeka na [onLoggedIn] ([loginExpired]). Zgłoszenia Zbierającego trafiają też do
 * [history]. Chwile zapisu z [SubmissionStamps.nowMillis] rosną ściśle, a serwer jest wołany poza blokadą wierszy.
 *
 * Wymagania: NFR-5, US9.1, US9.2, BR-9, NFR-8, NFR-6, US8.3, BR-10, BR-26, US14.4, US8
 */
class SubmissionQueue(
    private val store: PendingSubmissionDao,
    private val ioContext: CoroutineContext = Dispatchers.IO,
    private val server: SubmissionServer,
    private val stamps: SubmissionStamps = SubmissionStamps(),
    private val onSaved: () -> Unit = {},
    private val history: OwnSubmissionDao? = null,
) {
    private val sending = Mutex()
    private val results = SentResults(store = store, history = history, ioContext = ioContext)
    private val lastSavedAt = AtomicLong(Long.MIN_VALUE)
    private val expired = MutableStateFlow(false)

    /** `true` po odpowiedzi 401 aż do [onLoggedIn]: aplikacja prosi o ponowne zalogowanie. Wymagania: BR-26 */
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
        stamps = SubmissionStamps(newId = newId),
        onSaved = onSaved,
    )

    /**
     * Zapisuje zgłoszenie w kolejce z nowym identyfikatorem w telefonie.
     * Awaria planowania wysyłki nie może zgubić zapisanego zgłoszenia.
     *
     * Wymagania: BR-9, NFR-5, BR-7, BR-8
     *
     * @param content treść zgłoszenia
     * @param photos zdjęcia do wysłania; identyfikatory wracają w [content]
     * @throws SaveRefusedException zdjęcia nie odpowiadają treści
     */
    suspend fun save(content: SubmissionContent, photos: List<PendingPhotoDraft>): SavedSubmission {
        requireMatchingPhotos(content, photos)
        val id = stamps.newId()
        val savedAt = lastSavedAt.nextAfter(stamps.nowMillis())
        val entity = content.toPendingSubmissionEntity(id = id, savedAtMillis = savedAt)
        store.together {
            store.save(entity, photos.map { it.toPendingPhotoEntity(submissionId = id) })
            remember(entity)
        }
        runCatching(onSaved)
        return SavedSubmission(id = id)
    }

    /**
     * Sprzątanie przy starcie: usuwa z [dir] pliki zdjęć, których nie ma w kolejce ani w [kept].
     *
     * Wymagania: NFR-5
     *
     * @param dir katalog zdjęć zgłoszeń
     * @param kept pliki zdjęć odtworzonych szkiców
     */
    suspend fun deleteOrphanPhotos(dir: File, kept: Set<String>) {
        deleteOrphanPhotos(store = store, dir = dir, kept = kept, nowMillis = stamps.nowMillis(), ioContext = ioContext)
    }

    /**
     * Zapisuje poprawkę zgłoszenia z C3 pod jego identyfikatorem, więc kolejka nie ma dwóch wersji.
     *
     * Wymagania: US8.3, BR-9, BR-7, BR-8, BR-10
     *
     * @param edited poprawiane zgłoszenie
     * @param content nowa treść zgłoszenia
     * @param photos zdjęcia do wysłania; identyfikatory wracają w [content]
     * @throws SaveRefusedException zdjęcia nie odpowiadają treści albo serwer ma już zgłoszenie, a telefon nie zna
     *   jego wersji
     */
    suspend fun saveEdit(edited: EditedSubmission, content: SubmissionContent, photos: List<PendingPhotoDraft>) {
        requireMatchingPhotos(content, photos)
        val replaced =
            results.locked {
                val savedAt = lastSavedAt.nextAfter(stamps.nowMillis())
                val baseVersion = baseVersionOf(edited = edited, store = store, history = history)
                val fresh = content.toPendingSubmissionEntity(id = edited.id, savedAtMillis = savedAt)
                val entity = fresh.copy(baseVersion = baseVersion)
                var paths = emptyList<String>()
                store.together {
                    paths = store.save(entity, photos.map { it.toPendingPhotoEntity(submissionId = edited.id) })
                    remember(entity)
                }
                paths
            }
        deletePhotoFiles(replaced, ioContext)
        runCatching(onSaved)
    }

    /**
     * Wysyła zgłoszenia od najstarszych: treść raz, potem tylko nie wysłane zdjęcia. Bez sieci, przy 5xx albo po 401
     * kończy do kolejnej próby albo zalogowania. Inna odmowa (4xx) i wiersz, którego telefon nie odczyta, zostawiają
     * zgłoszenie jako odrzucone do ponownego zapisu ([saveEdit]), a wysyłka idzie dalej. Kolejka jest czytana po każdym
     * zgłoszeniu, więc zapisane w trakcie idzie w tym samym przebiegu; każde jest próbowane raz na przebieg, a poprawka
     * zapisana w trakcie wysyłki tego zgłoszenia idzie zaraz po niej.
     *
     * Wymagania: BR-9, BR-26, NFR-5, US9.2
     *
     * @return `false`, gdy trzeba spróbować później; `true`, gdy wysyłka doszła do końca kolejki albo czeka
     *   na zalogowanie, a nie na czas
     */
    suspend fun sendAll(): Boolean =
        sending.withLock {
            val stop = if (expired.value) Outcome.LOGIN_EXPIRED else sendUntilStop()
            if (stop == Outcome.LOGIN_EXPIRED) expired.value = true
            stop != Outcome.LATER
        }

    /** Po ponownym zalogowaniu wysyłka rusza sama, bez działania Zbierającego. Wymagania: BR-26, US14.4 */
    fun onLoggedIn() {
        expired.value = false
        runCatching(onSaved)
    }

    private suspend fun remember(entity: PendingSubmissionEntity) {
        if (entity.isOwnHistory()) history?.save(entity.toOwnSubmissionEntity())
    }

    private suspend fun sendUntilStop(): Outcome? {
        val tried = mutableSetOf<String>()
        var stop: Outcome? = null
        var next = store.all().firstOrNull()
        while (next != null && stop == null) {
            val outcome = sendOne(next)
            if (outcome != Outcome.REPLACED) tried += next.submission.id
            stop = outcome.takeIf { it == Outcome.LATER || it == Outcome.LOGIN_EXPIRED }
            next = store.all().firstOrNull { it.submission.id !in tried }
        }
        return stop
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
        val content = item.toSubmissionContentOrNull() ?: return results.unreadable(item)
        val result: ApiResult<*>
        val outcome =
            if (version == null) {
                result = server.create(item.submission.id, content)
                outcomeOf(result)
            } else {
                val edit = server.update(item.submission.id, version, content)
                result = edit
                editOutcome(edit)
            }
        return results.content(item, result, outcome)
    }

    private suspend fun sendRemaining(item: PendingSubmissionWithPhotos): Outcome {
        for (photo in item.photos.filterNot { it.isUploaded }) {
            val jpeg = jpeg(photo.path, ioContext)
            val result = jpeg?.let { server.putPhoto(item.submission.id, photo.photoId, it) }
            val outcome = results.photo(item, photo.photoId, result)
            if (outcome != Outcome.SENT) return outcome
        }
        return results.finish(item)
    }
}

/** Plik zdjęcia do wysłania; `null`, gdy telefon go nie odczyta. */
internal suspend fun jpeg(path: String, ioContext: CoroutineContext): ByteArray? =
    withContext(ioContext) {
        try {
            File(path).readBytes()
        } catch (_: IOException) {
            null
        }
    }

/**
 * Usuwa pliki zdjęć, których kolejka już nie potrzebuje: po wysłaniu całego zgłoszenia albo po zastąpieniu zdjęcia
 * poprawką. Pliki zgłoszenia czekającego zostają, bo poprawka z C3 może je jeszcze pokazać i wysłać.
 */
internal suspend fun deletePhotoFiles(paths: List<String>, ioContext: CoroutineContext) {
    if (paths.isNotEmpty()) withContext(ioContext) { paths.forEach { File(it).delete() } }
}

/**
 * Wersja bazowa poprawki [edited]: z C3, z poprzedniej poprawki w kolejce albo z historii po utworzeniu. Gdy serwer
 * ma już zgłoszenie, a wersja nie jest znana, zapis jest odrzucony, bo utworzenie z tym samym identyfikatorem
 * byłoby duplikatem; zgłoszenie tylko w telefonie zostaje bez wersji i idzie jako utworzenie.
 *
 * Wymagania: BR-9, BR-10
 */
internal suspend fun baseVersionOf(
    edited: EditedSubmission,
    store: PendingSubmissionDao,
    history: OwnSubmissionDao?,
): Int? {
    val pending = store.all().firstOrNull { it.submission.id == edited.id }?.submission
    val own = history?.find(edited.id)
    val version = edited.baseVersion ?: pending?.baseVersion ?: own?.serverVersion
    val isOnServer = pending?.isCreatedOnServer == true || own?.state == OwnSubmissionState.SENT
    if (version == null && isOnServer) {
        throw SaveRefusedException(
            reason = SaveRefusal.UNKNOWN_VERSION,
            message = "Serwer ma już zgłoszenie ${edited.id}, a wersja jest nieznana (BR-10)",
        )
    }
    return version
}

internal fun requireMatchingPhotos(content: SubmissionContent, photos: List<PendingPhotoDraft>) {
    if (photos.map { it.photoId } != content.photoIds) {
        throw SaveRefusedException(
            reason = SaveRefusal.PHOTOS_MISMATCH,
            message = "Zdjęcia w kolejce muszą odpowiadać identyfikatorom treści (BR-7, BR-8)",
        )
    }
}

internal fun outcomeOf(result: ApiResult<*>): Outcome =
    when (result) {
        is ApiResult.NetworkError -> Outcome.LATER
        is ApiResult.ServerError -> serverOutcome(result.code)
        ApiResult.NotFound -> Outcome.REFUSED
        is ApiResult.Success -> Outcome.SENT
    }

/**
 * Wynik edycji [result]: konflikt wersji (409) zostaje w telefonie jako „Nie wysłano”, bez ponawiania w kółko.
 * Wersja z serwera trafia do wiersza kolejki i historii, więc ponowny zapis poprawki idzie od niej.
 *
 * Wymagania: US9.3, BR-10
 */
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

internal fun AtomicLong.nextAfter(now: Long): Long = updateAndGet { last -> maxOf(a = now, b = last + 1) }
