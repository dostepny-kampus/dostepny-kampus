/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.annotation.StringRes
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.Severity

/** Nazwa typu bariery ze słownika projektu, np. „Próg” dla TB-13. Wymagania: BR-7 */
@StringRes
public fun BarrierType.nameRes(): Int = TYPE_NAMES.getValue(this)

/** Nazwa grupy barier, w której formularz C2 pokazuje typy. */
@StringRes
public fun BarrierGroup.nameRes(): Int =
    when (this) {
        BarrierGroup.ENTRANCE_AND_DOORS -> R.string.grupa_barier_wejscie
        BarrierGroup.VERTICAL_CIRCULATION -> R.string.grupa_barier_pionowa
        BarrierGroup.HORIZONTAL_CIRCULATION -> R.string.grupa_barier_pozioma
        BarrierGroup.TOILETS -> R.string.grupa_barier_toalety
        BarrierGroup.ORIENTATION_AND_INFORMATION -> R.string.grupa_barier_orientacja
        BarrierGroup.OTHER -> R.string.grupa_barier_inne
    }

/** Nazwa uciążliwości: „Utrudnienie”, „Tylko z pomocą” albo „Blokada”. Wymagania: US7.1 */
@StringRes
public fun Severity.nameRes(): Int =
    when (this) {
        Severity.HINDRANCE -> R.string.uciazliwosc_utrudnienie
        Severity.ONLY_WITH_HELP -> R.string.uciazliwosc_pomoc
        Severity.BLOCKING -> R.string.uciazliwosc_blokada
    }

/** Opis uciążliwości pod nazwą na C2, oceniany dla osoby, dla której bariera jest najtrudniejsza (PW-13). */
@StringRes
public fun Severity.descriptionRes(): Int =
    when (this) {
        Severity.HINDRANCE -> R.string.uciazliwosc_utrudnienie_opis
        Severity.ONLY_WITH_HELP -> R.string.uciazliwosc_pomoc_opis
        Severity.BLOCKING -> R.string.uciazliwosc_blokada_opis
    }

private val TYPE_NAMES: Map<BarrierType, Int> =
    mapOf(
        BarrierType.STAIRS_AT_ENTRANCE to R.string.typ_bariery_1,
        BarrierType.NARROW_DOOR to R.string.typ_bariery_2,
        BarrierType.HEAVY_DOOR to R.string.typ_bariery_3,
        BarrierType.NO_SPACE_BEFORE_DOOR to R.string.typ_bariery_4,
        BarrierType.DEVICE_OUT_OF_REACH to R.string.typ_bariery_5,
        BarrierType.STAIRS_WITHOUT_HANDRAIL to R.string.typ_bariery_6,
        BarrierType.STEPS_ON_FLOOR to R.string.typ_bariery_7,
        BarrierType.NO_LIFT to R.string.typ_bariery_8,
        BarrierType.BROKEN_LIFT to R.string.typ_bariery_9,
        BarrierType.UNADAPTED_LIFT to R.string.typ_bariery_10,
        BarrierType.UNADAPTED_RAMP to R.string.typ_bariery_11,
        BarrierType.PLATFORM_LIFT_NEEDS_HELP to R.string.typ_bariery_12,
        BarrierType.THRESHOLD to R.string.typ_bariery_13,
        BarrierType.NARROW_PASSAGE to R.string.typ_bariery_14,
        BarrierType.OBSTACLE_IN_PASSAGE to R.string.typ_bariery_15,
        BarrierType.SURFACE to R.string.typ_bariery_16,
        BarrierType.CURB to R.string.typ_bariery_17,
        BarrierType.NO_ACCESSIBLE_TOILET to R.string.typ_bariery_18,
        BarrierType.UNADAPTED_TOILET to R.string.typ_bariery_19,
        BarrierType.TOILET_INACCESSIBLE_IN_PRACTICE to R.string.typ_bariery_20,
        BarrierType.SIGNAGE to R.string.typ_bariery_21,
        BarrierType.NO_TACTILE_PLAN to R.string.typ_bariery_22,
        BarrierType.NO_CONTRAST to R.string.typ_bariery_23,
        BarrierType.NO_TACTILE_MARKINGS to R.string.typ_bariery_24,
        BarrierType.NO_HEARING_SUPPORT to R.string.typ_bariery_25,
        BarrierType.NO_PARKING_SPACE to R.string.typ_bariery_26,
        BarrierType.NO_EVACUATION_MEANS to R.string.typ_bariery_27,
        BarrierType.INACCESSIBLE_SERVICE_POINT to R.string.typ_bariery_28,
        BarrierType.OTHER_BARRIER to R.string.typ_bariery_29,
    )
