/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.own

import io.github.dostepnykampus.core.database.submission.OwnSubmissionDao
import io.github.dostepnykampus.core.database.submission.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import io.github.dostepnykampus.core.database.submission.PendingSubmissionDao
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

/** Stan wysyłki własnego zgłoszenia, te same nazwy co w bazie. Wymagania: US8.1, NFR-5 */
public enum class SendState {
    /** „W telefonie”: czeka na wysłanie. */
    LOCAL,

    /** „Wysłane”: serwer ma treść i zdjęcia. */
    SENT,

    /** „Nie wysłano”: serwer odmówił. */
    REFUSED,
}

/** Powód „Nie wysłano” na C3; ekran zna tylko powód, nie kody HTTP. Wymagania: US9.5, BR-26 */
public enum class RefusalReason {
    /** Serwer odebrał rolę (403): „Nie masz już uprawnień do tej czynności…”. */
    NO_PERMISSION,

    /** Inna odmowa: „Serwer nie przyjął zgłoszenia. Otwórz je, popraw i zapisz ponownie.” */
    NOT_ACCEPTED,

    /** Odmowa bez kodu: telefon nie odczytał zgłoszenia, np. brakuje pliku zdjęcia. */
    UNREADABLE,

    /** Konflikt wersji (409): ktoś zmienił zgłoszenie na serwerze po rozpoczęciu poprawki. Wymagania: US9.3, BR-10 */
    CHANGED_ON_SERVER,
}

/**
 * Własne zgłoszenie z historii w telefonie.
 *
 * Wymagania: US8.1, BR-9, US8.2, BR-16
 *
 * @property id identyfikator zgłoszenia
 * @property savedAt chwila ostatniego zapisu w telefonie
 * @property barrierType kod typu bariery TB-1…TB-29
 * @property buildingSymbol budynek; `null` to „poza budynkiem”
 * @property floorNumber numer piętra budynku podstawowego
 * @property state stan wysyłki
 * @property refusal powód „Nie wysłano”; `null` bez odmowy
 * @property serverStatus status z serwera po odświeżeniu; `null`, gdy telefon go jeszcze nie zna
 * @property rejectionReason powód odrzucenia od Weryfikatora; `null` poza odrzuceniem
 * @property isChangeWaiting serwer ma zgłoszenie, a jego zmiana czeka w kolejce telefonu na wysłanie
 */
public data class OwnSubmission(
    val id: String,
    val savedAt: Instant,
    val barrierType: String?,
    val buildingSymbol: String?,
    val floorNumber: Int?,
    val state: SendState,
    val refusal: RefusalReason?,
    val serverStatus: SubmissionStatus? = null,
    val rejectionReason: String? = null,
    val isChangeWaiting: Boolean = false,
)

/** Własne zgłoszenia od najnowszego; w aplikacji z historii Room. Wymagania: US8.1, NFR-5 */
public fun interface OwnSubmissions {
    /** Lista od najnowszego; zmienia się po każdym zapisie i wysyłce z kolejki. */
    public fun observe(): Flow<List<OwnSubmission>>
}

/**
 * Własne zgłoszenia z historii [history] konta z [subjects]. Bez zalogowanego konta lista jest pusta.
 * Zmiana czekająca w kolejce [pending] liczy się tylko dla tego samego konta.
 *
 * Wymagania: US8.1, NFR-33
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun ownSubmissionsOf(
    history: OwnSubmissionDao,
    pending: PendingSubmissionDao,
    subjects: Flow<String?>,
): OwnSubmissions =
    OwnSubmissions {
        subjects.flatMapLatest { owner ->
            if (owner == null) {
                flowOf(emptyList())
            } else {
                combine(history.observe(owner), pending.observe()) { rows, queued ->
                    val waiting = queued.filter { it.submission.ownerSub == owner }.map { it.submission.id }.toSet()
                    rows.map { it.toOwnSubmission(isQueued = it.id in waiting) }
                }
            }
        }
    }

internal fun OwnSubmissionEntity.toOwnSubmission(isQueued: Boolean = false): OwnSubmission =
    OwnSubmission(
        id = id,
        savedAt = Instant.ofEpochMilli(savedAtMillis),
        barrierType = barrierType,
        buildingSymbol = buildingSymbol,
        floorNumber = floorNumber,
        state = SendState.valueOf(state.name),
        refusal = refusalReasonOf(state, refusalCode),
        serverStatus = SubmissionStatus.entries.find { it.name == serverStatus },
        rejectionReason = rejectionReason,
        isChangeWaiting = isQueued && state == OwnSubmissionState.SENT,
    )
