/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.marking

import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind

/** Etap ekranu C6 „Zmiana oznaczenia wejścia”. Wymagania: US17.2 */
enum class MarkingStage {
    /** Wejście jest wczytywane. */
    LOADING,

    /** Wejścia nie ma w katalogu budynków; ekran wraca do poprzedniego. */
    MISSING,

    /** Wejście otwarte; czeka na wybór, zdjęcie i wysłanie. */
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
 * Stan ekranu C6.
 *
 * Wymagania: US17.2, US17.3, BR-24
 *
 * @property entrance wejście z obecnym oznaczeniem, datą i źródłem; `null` przed wczytaniem albo gdy go nie ma
 * @property buildingName nazwa budynku wejścia z katalogu; `null`, gdy budynek jej nie ma
 * @property chosen nowe oznaczenie: dostępne albo niedostępne; `null` przed wyborem
 * @property photo zdjęcie wejścia albo `null`
 * @property isChoiceMissing czy wysłanie bez wyboru zostało już odrzucone; znika po wyborze
 * @property isPhotoMissing czy wysłanie bez zdjęcia zostało już odrzucone; znika po zrobieniu zdjęcia
 * @property stage etap ekranu
 */
data class MarkingUiState(
    val entrance: Entrance? = null,
    val buildingName: String? = null,
    val chosen: EntranceMarking? = null,
    val photo: PendingPhotoDraft? = null,
    val isChoiceMissing: Boolean = false,
    val isPhotoMissing: Boolean = false,
    val stage: MarkingStage = MarkingStage.LOADING,
)

internal val MarkingUiState.isEditable: Boolean
    get() = entrance != null && stage in EditableStages

private val EditableStages = setOf(MarkingStage.EDITING, MarkingStage.SAVE_FAILED, MarkingStage.SAVE_REJECTED)

/**
 * Treść zmiany oznaczenia wejścia na [marking]: wejście, jego budynek i punkt, bez typu bariery i uciążliwości.
 *
 * Wymagania: US17.2, BR-24, BR-7
 */
internal fun Entrance.markingContent(marking: EntranceMarking, photo: PendingPhotoDraft): SubmissionContent =
    SubmissionContent(
        kind = SubmissionKind.ENTRANCE_MARKING,
        location = location,
        photoIds = listOf(photo.photoId),
        buildingSymbol = buildingSymbol,
        entranceId = id,
        entranceMarking = marking,
    )
