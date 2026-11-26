/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.removed

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.model.building.EntranceDistance
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind

/** Etap ekranu C7 „Usunięcie bariery”. Wymagania: US8.4 */
enum class RemovedStage {
    /** Zgłoszenie przyjęte jest wczytywane. */
    LOADING,

    /** Zgłoszenia przyjętego nie ma, np. przestało być przyjęte; ekran wraca do poprzedniego. */
    MISSING,

    /** Zgłoszenie otwarte; czeka na nowe zdjęcie i wysłanie. */
    EDITING,

    /** Zapis trwa; wysłanie jest nieczynne, żeby nie zapisać dwa razy. */
    SAVING,

    /** „Za mało miejsca w telefonie”, ponowne wysłanie możliwe. Wymagania: NFR-7 */
    SAVE_FAILED,

    /** Kolejka odrzuciła wpis, bo zdjęcie nie pasuje do treści. Wymagania: NFR-50 */
    SAVE_REJECTED,

    /** Zapisane w telefonie do wysłania. Wymagania: NFR-54 */
    SAVED,
}

/**
 * Stan ekranu C7.
 *
 * Wymagania: US8.4, BR-19
 *
 * @property previous zgłoszenie przyjęte, którego dotyczy zmiana; `null` przed wczytaniem albo gdy go nie ma
 * @property buildingName nazwa budynku bariery z katalogu; `null`, gdy budynek jej nie ma albo nie ma go w katalogu
 * @property entranceName nazwa wejścia przy barierze z katalogu; `null`, gdy zgłoszenie go nie wskazuje
 * @property nearest najbliższe wejście budynku bariery z odległością; `null`, gdy katalog nie ma wejść budynku
 * @property photo nowe zdjęcie miejsca albo `null`
 * @property isPhotoMissing czy wysłanie bez zdjęcia zostało już odrzucone; znika po zrobieniu zdjęcia
 * @property stage etap ekranu
 */
data class RemovedUiState(
    val previous: AcceptedBarrier? = null,
    val buildingName: String? = null,
    val entranceName: String? = null,
    val nearest: EntranceDistance? = null,
    val photo: PendingPhotoDraft? = null,
    val isPhotoMissing: Boolean = false,
    val stage: RemovedStage = RemovedStage.LOADING,
)

internal val RemovedUiState.isEditable: Boolean
    get() = previous != null && stage in EditableStages

private val EditableStages = setOf(RemovedStage.EDITING, RemovedStage.SAVE_FAILED, RemovedStage.SAVE_REJECTED)

/**
 * Treść zgłoszenia „bariera usunięta”: typ, miejsce i piętro przechodzą z poprzedniego zgłoszenia, uciążliwości nie ma.
 *
 * Wymagania: US8.4, BR-19, BR-7
 */
internal fun AcceptedBarrier.removalContent(photo: PendingPhotoDraft): SubmissionContent =
    SubmissionContent(
        kind = SubmissionKind.PRECISE,
        location = location,
        photoIds = listOf(photo.photoId),
        buildingSymbol = buildingSymbol,
        floor = floor,
        barrierGroup = type?.group,
        barrierType = type?.code,
        replacesSubmissionId = id,
        isBarrierRemoved = true,
    )
