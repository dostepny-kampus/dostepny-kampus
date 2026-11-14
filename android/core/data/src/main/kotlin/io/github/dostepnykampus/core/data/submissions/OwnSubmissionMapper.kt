/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.data.server.ServerEdit
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.database.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.OwnSubmissionState
import io.github.dostepnykampus.core.database.PendingSubmissionEntity
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionKind

private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_CONFLICT = 409

/** Czy zgłoszenie trafia na listę C3; szybkie zgłoszenie Odwiedzającego nie trafia. Wymagania: US4.4, US8 */
internal fun PendingSubmissionEntity.isOwnHistory(): Boolean = kind != SubmissionKind.QUICK.name

/** Wiersz listy C3 ze stanem „W telefonie” dla zgłoszenia zapisanego w kolejce. Wymagania: US8, NFR-5 */
internal fun PendingSubmissionEntity.toOwnSubmissionEntity(): OwnSubmissionEntity =
    OwnSubmissionEntity(
        id = id,
        savedAtMillis = savedAtMillis,
        kind = kind,
        buildingSymbol = buildingSymbol,
        floorNumber = floorNumber,
        floorId = floorId,
        barrierType = barrierType,
        severity = severity,
    )

/** Wersja z serwera po przyjęciu treści; `null`, gdy serwer nie przyjął treści. Wymagania: BR-10 */
internal fun versionOf(result: ServerReply<*>): Int? {
    val value = (result as? ServerReply.Ok)?.value
    return when (value) {
        is Submission -> value.version
        is ServerEdit.Saved -> value.submission.version
        else -> null
    }
}

/** Wersja z serwera z odpowiedzi 409 na edycję; `null` przy innym wyniku. Wymagania: US9.3, BR-10 */
internal fun conflictVersionOf(result: ServerReply<*>): Int? {
    val conflict = (result as? ServerReply.Ok)?.value as? ServerEdit.Conflict
    return conflict?.run { server.version }
}

/** Kod odpowiedzi HTTP odmowy do pokazania przy „Nie wysłano”; `null`, gdy serwer nie odmówił. */
internal fun refusalCodeOf(result: ServerReply<*>): Int? =
    when (result) {
        is ServerReply.Fault -> result.code
        ServerReply.Missing -> HTTP_NOT_FOUND
        is ServerReply.Ok -> HTTP_CONFLICT.takeIf { result.value is ServerEdit.Conflict }
        ServerReply.Offline -> null
    }

/**
 * Powód „Nie wysłano” z kodu odmowy zapisanego w historii obok [refusalCodeOf], jedyne miejsce, które zna znaczenie
 * kodów: 403 to brak uprawnień, 409 konflikt wersji, brak kodu to zgłoszenie, którego telefon nie odczytał, a inne
 * kody, także 404, to „Serwer nie przyjął zgłoszenia”.
 *
 * Wymagania: US9.5, BR-10, BR-26
 */
internal fun refusalReasonOf(state: OwnSubmissionState, code: Int?): RefusalReason? =
    when {
        state != OwnSubmissionState.REFUSED -> null
        code == null -> RefusalReason.UNREADABLE
        code == HTTP_FORBIDDEN -> RefusalReason.NO_PERMISSION
        code == HTTP_CONFLICT -> RefusalReason.CHANGED_ON_SERVER
        else -> RefusalReason.NOT_ACCEPTED
    }
