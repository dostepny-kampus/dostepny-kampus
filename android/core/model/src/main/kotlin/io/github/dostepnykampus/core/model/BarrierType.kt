/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

/**
 * Typ bariery z klasyfikacji TB-1…TB-29 w słowniku projektu, przypisany do jednej z sześciu grup barier (BR-7).
 * Nazwy dla ludzi są w zasobach ekranów; tu jest tylko kod kontraktu i grupa.
 *
 * @property code kod typu wysyłany w polu `barrierType` zgłoszenia, np. `TB-13`
 * @property group grupa barier, w której formularz C2 pokazuje typ
 */
enum class BarrierType(val code: String, val group: BarrierGroup) {
    /** Schody przy wejściu. */
    STAIRS_AT_ENTRANCE("TB-1", BarrierGroup.ENTRANCE_AND_DOORS),

    /** Wąskie drzwi. */
    NARROW_DOOR("TB-2", BarrierGroup.ENTRANCE_AND_DOORS),

    /** Ciężkie drzwi. */
    HEAVY_DOOR("TB-3", BarrierGroup.ENTRANCE_AND_DOORS),

    /** Brak miejsca przed drzwiami. */
    NO_SPACE_BEFORE_DOOR("TB-4", BarrierGroup.ENTRANCE_AND_DOORS),

    /** Urządzenie poza zasięgiem. */
    DEVICE_OUT_OF_REACH("TB-5", BarrierGroup.ENTRANCE_AND_DOORS),

    /** Schody bez poręczy. */
    STAIRS_WITHOUT_HANDRAIL("TB-6", BarrierGroup.VERTICAL_CIRCULATION),

    /** Stopnie na piętrze. */
    STEPS_ON_FLOOR("TB-7", BarrierGroup.VERTICAL_CIRCULATION),

    /** Brak windy. */
    NO_LIFT("TB-8", BarrierGroup.VERTICAL_CIRCULATION),

    /** Niesprawna winda. */
    BROKEN_LIFT("TB-9", BarrierGroup.VERTICAL_CIRCULATION),

    /** Winda nieprzystosowana. */
    UNADAPTED_LIFT("TB-10", BarrierGroup.VERTICAL_CIRCULATION),

    /** Pochylnia nieprzystosowana. */
    UNADAPTED_RAMP("TB-11", BarrierGroup.VERTICAL_CIRCULATION),

    /** Podnośnik wymagający pomocy. */
    PLATFORM_LIFT_NEEDS_HELP("TB-12", BarrierGroup.VERTICAL_CIRCULATION),

    /** Próg. */
    THRESHOLD("TB-13", BarrierGroup.HORIZONTAL_CIRCULATION),

    /** Wąskie przejście. */
    NARROW_PASSAGE("TB-14", BarrierGroup.HORIZONTAL_CIRCULATION),

    /** Przeszkoda w przejściu. */
    OBSTACLE_IN_PASSAGE("TB-15", BarrierGroup.HORIZONTAL_CIRCULATION),

    /** Nawierzchnia. */
    SURFACE("TB-16", BarrierGroup.HORIZONTAL_CIRCULATION),

    /** Krawężnik. */
    CURB("TB-17", BarrierGroup.HORIZONTAL_CIRCULATION),

    /** Brak toalety dostępnej. */
    NO_ACCESSIBLE_TOILET("TB-18", BarrierGroup.TOILETS),

    /** Toaleta nieprzystosowana. */
    UNADAPTED_TOILET("TB-19", BarrierGroup.TOILETS),

    /** Toaleta niedostępna w praktyce. */
    TOILET_INACCESSIBLE_IN_PRACTICE("TB-20", BarrierGroup.TOILETS),

    /** Oznakowanie. */
    SIGNAGE("TB-21", BarrierGroup.ORIENTATION_AND_INFORMATION),

    /** Brak planu tyflograficznego. */
    NO_TACTILE_PLAN("TB-22", BarrierGroup.ORIENTATION_AND_INFORMATION),

    /** Brak kontrastu. */
    NO_CONTRAST("TB-23", BarrierGroup.ORIENTATION_AND_INFORMATION),

    /** Brak oznaczeń dotykowych. */
    NO_TACTILE_MARKINGS("TB-24", BarrierGroup.ORIENTATION_AND_INFORMATION),

    /** Brak wsparcia słuchu. */
    NO_HEARING_SUPPORT("TB-25", BarrierGroup.ORIENTATION_AND_INFORMATION),

    /** Brak miejsca parkingowego. */
    NO_PARKING_SPACE("TB-26", BarrierGroup.OTHER),

    /** Brak środków ewakuacji. */
    NO_EVACUATION_MEANS("TB-27", BarrierGroup.OTHER),

    /** Niedostępne miejsce obsługi. */
    INACCESSIBLE_SERVICE_POINT("TB-28", BarrierGroup.OTHER),

    /** Inna bariera. */
    OTHER_BARRIER("TB-29", BarrierGroup.OTHER),
    ;

    /** Opis jest obowiązkowy tylko przy TB-29 „inna bariera” (US7.3). */
    val isDescriptionRequired: Boolean
        get() = this == OTHER_BARRIER

    /** Wyszukiwanie typu po kodzie z kontraktu. */
    companion object {
        /** Typ o kodzie z kontraktu albo `null`, gdy serwer lub kolejka podały kod spoza klasyfikacji. */
        fun fromCode(code: String): BarrierType? = entries.firstOrNull { it.code == code }
    }
}

/**
 * Uciążliwość bariery 1–3 oceniana dla osoby, dla której bariera jest najtrudniejsza (US7.1, PW-13).
 *
 * @property value wartość pola `severity` w kontrakcie
 */
enum class Severity(val value: Int) {
    /** „Utrudnienie”. */
    HINDRANCE(1),

    /** „Tylko z pomocą”. */
    ONLY_WITH_HELP(2),

    /** „Blokada”. */
    BLOCKING(BLOCKING_VALUE),
    ;

    /** Wyszukiwanie uciążliwości po wartości z kontraktu. */
    companion object {
        /** Uciążliwość o wartości z kontraktu albo `null` spoza zakresu 1–3. */
        fun fromValue(value: Int): Severity? = entries.firstOrNull { it.value == value }
    }
}

private const val BLOCKING_VALUE = 3
