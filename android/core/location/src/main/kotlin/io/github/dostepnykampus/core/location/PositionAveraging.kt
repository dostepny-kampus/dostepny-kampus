/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import io.github.dostepnykampus.core.model.GeoPoint
import kotlin.math.ceil
import kotlin.math.sqrt

/** Czas uśredniania pozycji na ekranie C1 (US6.1, NFR-1). */
const val AVERAGING_MILLIS: Long = 30_000

private const val MILLIS_IN_SECOND = 1_000.0

/**
 * Odczyt położenia z GNSS (US6.1).
 *
 * @property point punkt w WGS84
 * @property accuracyMeters szacowany promień ufności 68% w metrach, jak podaje system (NFR-2)
 * @property elapsedMillis czas odczytu na zegarze monotonicznym telefonu, tym samym co start pomiaru
 */
data class PositionFix(val point: GeoPoint, val accuracyMeters: Double, val elapsedMillis: Long)

/**
 * Średnia pozycja z odczytów pomiaru.
 *
 * @property point średnia ważona odczytów
 * @property accuracyMeters szacowany promień ufności średniej w metrach; ekran pokazuje go jako szacunek (NFR-2)
 * @property fixCount liczba odczytów w średniej
 */
data class PositionAverage(val point: GeoPoint, val accuracyMeters: Double, val fixCount: Int)

/**
 * Pomiar na ekranie C1: odczyty GNSS z 30 s od startu uśrednione w jeden punkt (US6.1, NFR-1).
 *
 * Waga odczytu to odwrotność kwadratu jego promienia ufności, więc dokładniejszy odczyt ciągnie średnią mocniej.
 * Promień średniej to `1 / √Σw`; zakłada niezależne odczyty, a kolejne odczyty GNSS są ze sobą powiązane, więc to
 * szacunek z dołu, który sprawdza pomiar w terenie z NFR-1. Odczyty sprzed startu (ostatnia znana pozycja), po
 * końcu okna i bez znanej dokładności nie wchodzą do średniej. Stan jest niezmienny, więc ekran trzyma go we
 * `StateFlow` bez kopii.
 *
 * @property startMillis start pomiaru na zegarze monotonicznym telefonu
 * @param fixes odczyty z okna pomiaru w kolejności dołożenia
 */
class PositionAveraging private constructor(val startMillis: Long, private val fixes: List<PositionFix>) {
    private val endMillis: Long = startMillis + AVERAGING_MILLIS

    /** Nowy pomiar bez odczytów, startujący w [startMillis]. */
    constructor(startMillis: Long) : this(startMillis = startMillis, fixes = emptyList())

    /** Pomiar z dołożonym odczytem, gdy ten mieści się w oknie i ma znaną dokładność. */
    fun withFix(fix: PositionFix): PositionAveraging =
        if (fix.elapsedMillis in startMillis until endMillis && fix.isUsable()) {
            PositionAveraging(startMillis, fixes + fix)
        } else {
            this
        }

    /** Pełne sekundy do końca pomiaru, zaokrąglone w górę, jak w napisie „Pomiar: zostało 12 s”. */
    fun remainingSeconds(nowMillis: Long): Int =
        ceil((endMillis - nowMillis).coerceAtLeast(0) / MILLIS_IN_SECOND).toInt()

    /** Czy minęło 30 s; wtedy ekran pokazuje „Pomiar zakończony”. */
    fun isFinished(nowMillis: Long): Boolean = nowMillis >= endMillis

    /** „Powtórz pomiar”: nowe pełne 30 s bez odczytów tego pomiaru, bez limitu powtórzeń (NFR-23). */
    fun restart(startMillis: Long): PositionAveraging = PositionAveraging(startMillis)

    /** Średnia z dotychczasowych odczytów albo `null`, dopóki nie ma żadnego. */
    fun average(): PositionAverage? {
        if (fixes.isEmpty()) return null
        val weights = fixes.map { 1 / (it.accuracyMeters * it.accuracyMeters) }
        val total = weights.sum()
        val longitude = fixes.indices.sumOf { weights[it] * fixes[it].point.longitude } / total
        val latitude = fixes.indices.sumOf { weights[it] * fixes[it].point.latitude } / total
        return PositionAverage(
            point = GeoPoint(longitude = longitude, latitude = latitude),
            accuracyMeters = 1 / sqrt(total),
            fixCount = fixes.size,
        )
    }

    private fun PositionFix.isUsable(): Boolean = accuracyMeters.isFinite() && accuracyMeters > 0
}
