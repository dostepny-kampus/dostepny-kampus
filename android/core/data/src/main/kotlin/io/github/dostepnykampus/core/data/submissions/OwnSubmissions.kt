/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.OwnSubmissionEntity
import io.github.dostepnykampus.core.model.SubmissionStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Stan wysyłki własnego zgłoszenia, te same nazwy co w bazie (US8.1, NFR-5). */
enum class SendState {
    /** „W telefonie”: czeka na wysłanie. */
    LOCAL,

    /** „Wysłane”: serwer ma treść i zdjęcia. */
    SENT,

    /** „Nie wysłano”: serwer odmówił. */
    REFUSED,
}

/**
 * Własne zgłoszenie z historii w telefonie (US8.1).
 *
 * @property id identyfikator zgłoszenia (BR-9)
 * @property savedAt chwila ostatniego zapisu w telefonie
 * @property barrierType kod typu bariery TB-1…TB-29
 * @property buildingSymbol budynek; `null` to „poza budynkiem”
 * @property floorNumber numer piętra budynku podstawowego
 * @property state stan wysyłki
 * @property refusalCode kod odpowiedzi HTTP odmowy; `null`, gdy serwer nie odmówił albo nie podał kodu
 * @property serverStatus status z serwera po odświeżeniu (US8.2); `null`, gdy telefon go jeszcze nie zna
 * @property rejectionReason powód odrzucenia od Weryfikatora (BR-16); `null` poza odrzuceniem
 */
data class OwnSubmission(
    val id: String,
    val savedAt: Instant,
    val barrierType: String?,
    val buildingSymbol: String?,
    val floorNumber: Int?,
    val state: SendState,
    val refusalCode: Int?,
    val serverStatus: SubmissionStatus? = null,
    val rejectionReason: String? = null,
)

/** Własne zgłoszenia od najnowszego; w aplikacji z historii Room (US8.1, NFR-5). */
fun interface OwnSubmissions {
    /** Lista od najnowszego; zmienia się po każdym zapisie i wysyłce z kolejki. */
    fun observe(): Flow<List<OwnSubmission>>
}

internal fun OwnSubmissionEntity.toOwnSubmission(): OwnSubmission =
    OwnSubmission(
        id = id,
        savedAt = Instant.ofEpochMilli(savedAtMillis),
        barrierType = barrierType,
        buildingSymbol = buildingSymbol,
        floorNumber = floorNumber,
        state = SendState.valueOf(state.name),
        refusalCode = refusalCode,
        serverStatus = SubmissionStatus.entries.find { it.name == serverStatus },
        rejectionReason = rejectionReason,
    )
