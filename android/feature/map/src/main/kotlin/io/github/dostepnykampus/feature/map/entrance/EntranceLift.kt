/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.compose.runtime.Immutable
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.geo.metersTo
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.room.RoomCategory
import java.util.Locale
import kotlin.math.roundToInt

private const val STEP_METERS = 5
private val POLISH = Locale.forLanguageTag("pl")
private val NUMBERED_FLOOR = Regex("""-?\d+ piętro""")

/**
 * Najbliższa winda wejścia z planu PWr, do części „Za drzwiami”.
 *
 * Wymagania: US3.3, US16.3, BR-25
 *
 * @property entranceId identyfikator wejścia
 * @property meters odległość od wejścia w linii prostej, zaokrąglona do 5 m, najmniej 5 m
 * @property floors etykiety pięter windy z danych, od najniższego
 */
@Immutable
data class EntranceLift(val entranceId: String, val meters: Int, val floors: List<String>)

/**
 * Najbliższa winda [entrance] na jego piętrze, a bez piętra w danych na parterze; `null` bez planu budynku albo bez
 * windy na tym piętrze. Bez kopii planu pyta serwer raz. TODO(#3052): klatki przy windzie.
 */
internal suspend fun FloorPlans.liftNear(entrance: Entrance): EntranceLift? {
    val symbol = entrance.buildingSymbol
    val floors = (floors(symbol) ?: if (download(symbol)) floors(symbol) else null)?.floors.orEmpty()
    val floor = floors.firstOrNull { it.id == entrance.floorId } ?: floors.firstOrNull { it.order == 0 }
    val lifts =
        floor
            ?.let { rooms(symbol, it.id)?.rooms }
            .orEmpty()
            .filter { it.category == RoomCategory.ELEVATOR }
    val nearest = lifts.minByOrNull { it.labelPoint.metersTo(entrance.location) } ?: return null
    val steps = (nearest.labelPoint.metersTo(entrance.location) / STEP_METERS).roundToInt()
    val stops = floors.filter { it.id in nearest.liftFloors }.map { it.label }
    return EntranceLift(entranceId = entrance.id, meters = steps.coerceAtLeast(1) * STEP_METERS, floors = stops)
}

/**
 * Pierwsze i ostatnie piętro z [floors] w dopełniaczu, do zdania „Jeździ od piwnicy do 3 piętra.”; `null`, gdy pięter
 * jest mniej niż dwa albo któregoś skrajnego nie odmieniamy (odmieniamy piwnicę, parter i „N piętro”).
 */
internal fun liftRange(floors: List<String>): Pair<String, String>? {
    val from = floors.takeIf { it.size > 1 }?.first()?.let(::genitive)
    val to = floors.lastOrNull()?.let(::genitive)
    return if (from != null && to != null) from to to else null
}

/** Etykieta piętra małą literą, do listy „Jedzie na: piwnica, parter.”. */
internal fun String.floorWord(): String = lowercase(POLISH)

private fun genitive(label: String): String? {
    val name = label.floorWord()
    return when {
        name == "piwnica" -> "piwnicy"
        name == "parter" -> "parteru"
        NUMBERED_FLOOR.matches(name) -> name.replace("piętro", "piętra")
        else -> null
    }
}
