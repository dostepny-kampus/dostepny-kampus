/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.geo.toGeometry
import io.github.dostepnykampus.core.geo.toPosition
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.feature.map.campus.featureCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Point
import kotlin.coroutines.CoroutineContext

/** Źródło stylu z pomieszczeniami i ścianami wszystkich pięter budynku z planem. Wymagania: US16.1 */
internal const val PLAN_SOURCE = "plan"

/** Warstwa wypełnienia pomieszczeń wybranego piętra kolorem grupy funkcji. Wymagania: US16.1 */
internal const val PLAN_ROOMS_LAYER = "plan_pomieszczenia"

/** Warstwa ścian pomieszczeń wybranego piętra. Wymagania: US16.1 */
internal const val PLAN_WALLS_LAYER = "plan_sciany"

/** Warstwa ściany piętra z obrysu sumy jego pomieszczeń (R11). Wymagania: US16.1 */
internal const val PLAN_OUTLINE_LAYER = "plan_obrys"

/** Warstwa numerów sal wybranego piętra od powiększenia 19 (R12). Wymagania: US16.1 */
internal const val PLAN_NUMBERS_LAYER = "plan_numery"

/** Warstwa znaków windy, toalety dostosowanej i schodów na planie. Wymagania: US16.1 */
internal const val PLAN_SIGNS_LAYER = "plan_znaki"

/** Bok obrazu znaku na planie w dp. */
internal const val PLAN_SIGN_DP = 28

/** Grupy funkcji, które mają na planie znak zamiast samego koloru. Wymagania: US16.1 */
internal val PLAN_SIGN_CATEGORIES =
    setOf(RoomCategory.ELEVATOR, RoomCategory.ACCESSIBLE_TOILET, RoomCategory.STAIRCASE)

private const val FLOOR = "floor"
private const val KIND = "kind"
private const val CATEGORY = "category"
private const val ROOM = "room"
private const val OUTLINE = "outline"
private const val NUMBER = "number"
private const val SIGN = "sign"
private const val FILTER = "filter"
private const val IMAGE = "image"
private const val NUMBERS_MIN_ZOOM = 19.0
private const val WALL_WIDTH = 1.0
private const val OUTLINE_WIDTH = 3.0

/**
 * Kolory planu piętra z tokenów `mapa.kolory` jako `#RRGGBB`.
 *
 * @property classroom sala i sala wykładowa (`p_sala`)
 * @property corridor korytarz (`p_korytarz`)
 * @property other klatka schodowa, biuro i reszta (`p_inne`)
 * @property elevator winda (`p_winda`)
 * @property toilet toaleta (`p_wc`)
 * @property wall ściany pomieszczeń i piętra (`p_kontur`)
 */
data class PlanColors(
    val classroom: String,
    val corridor: String,
    val other: String,
    val elevator: String,
    val toilet: String,
    val wall: String,
)

/**
 * Obiekty planu pięter [floors] (identyfikator piętra → jego plan): pomieszczenie z właściwościami `floor`, `kind`
 * = `room` i `category`, ściana piętra z `floor` i `kind` = `outline`; piętro bez pomieszczeń nie ma ściany. Punkt
 * podpisu pomieszczenia z `labelled` ma `kind` = `number` i nazwę obrazu numeru `image` z [roomNumberImage]. Punkt
 * podpisu windy, toalety dostosowanej i klatki schodowej ma `kind` = `sign` i obraz znaku z [planSignImage].
 *
 * Wymagania: US16.1
 */
internal fun planFeatures(floors: Map<String, RoomList>): JsonElement =
    featureCollection(
        floors.flatMap { (floor, plan) ->
            val walls = plan.floorOutline.takeUnless { it is GeoMultiPolygon && it.polygons.isEmpty() }
            val outline = listOfNotNull(walls?.let { Feature(it.toGeometry(), part(floor, OUTLINE)) })
            val rooms = plan.rooms.map { room -> Feature(room.geometry.toGeometry(), part(floor, ROOM, room.category)) }
            val numbers =
                plan.rooms.filter { it.isLabelled }.mapNotNull { room ->
                    room.number?.let {
                        Feature(Point(room.labelPoint.toPosition()), part(floor, NUMBER, image = roomNumberImage(it)))
                    }
                }
            val signs =
                plan.rooms.filter { it.category in PLAN_SIGN_CATEGORIES }.map { room ->
                    val image = planSignImage(room.category)
                    Feature(Point(room.labelPoint.toPosition()), part(floor, SIGN, image = image))
                }
            outline + rooms + signs + numbers
        },
    )

/**
 * Warstwy planu w kolorach [colors]: pomieszczenia piętra [floor] według grupy funkcji, ich ściany, grubsza ściana
 * piętra, znaki windy, toalety dostosowanej i schodów oraz numery sal od powiększenia 19; numer albo znak, który
 * zachodzi na inny, znika. Bez piętra filtry nie przepuszczają
 * niczego.
 *
 * Wymagania: US16.1, NFR-16
 */
internal fun planLayers(colors: PlanColors, floor: String? = null): List<JsonObject> {
    val none = planFilter(floor = floor, kind = ROOM)
    val fill =
        styleLayer(id = PLAN_ROOMS_LAYER, type = "fill", source = PLAN_SOURCE) {
            put(key = FILTER, element = none)
            paint("fill-color" to categoryColor(colors))
        }
    return listOf(
        fill,
        lineLayer(id = PLAN_WALLS_LAYER, source = PLAN_SOURCE, color = colors.wall, width = WALL_WIDTH, filter = none),
        lineLayer(
            id = PLAN_OUTLINE_LAYER,
            source = PLAN_SOURCE,
            color = colors.wall,
            width = OUTLINE_WIDTH,
            filter = planFilter(floor = floor, kind = OUTLINE),
        ),
        JsonObject(
            symbolLayer(id = PLAN_SIGNS_LAYER, source = PLAN_SOURCE, image = property(IMAGE)) +
                (FILTER to planFilter(floor = floor, kind = SIGN)),
        ),
        JsonObject(
            symbolLayer(id = PLAN_NUMBERS_LAYER, source = PLAN_SOURCE, image = property(IMAGE)) +
                (FILTER to planFilter(floor = floor, kind = NUMBER)) +
                ("minzoom" to JsonPrimitive(NUMBERS_MIN_ZOOM)),
        ),
    )
}

/** Nazwa obrazu numeru sali [number]; przedrostek odróżnia go od podpisów budynków. Wymagania: US16.1 */
internal fun roomNumberImage(number: String): String = "sala: $number"

/** Nazwa obrazu znaku grupy funkcji [category] na planie. Wymagania: US16.1 */
internal fun planSignImage(category: RoomCategory): String = "znak planu: ${category.name}"

/**
 * Pokazuje w stylu z [planLayers] piętro [floor] planu już podanego przez [showPlanData]; bez piętra plan znika.
 * Zmienia tylko filtry warstw, więc zmiana piętra nie przelicza danych planu. Wymagania: US16.1, NFR-10
 */
internal fun MapStyleTarget.showPlanFloor(floor: String?) {
    filter(layer = PLAN_ROOMS_LAYER, filter = planFilter(floor = floor, kind = ROOM))
    filter(layer = PLAN_WALLS_LAYER, filter = planFilter(floor = floor, kind = ROOM))
    filter(layer = PLAN_OUTLINE_LAYER, filter = planFilter(floor = floor, kind = OUTLINE))
    filter(layer = PLAN_SIGNS_LAYER, filter = planFilter(floor = floor, kind = SIGN))
    filter(layer = PLAN_NUMBERS_LAYER, filter = planFilter(floor = floor, kind = NUMBER))
}

/**
 * Podaje stylowi z [planLayers] obiekty wszystkich pięter planu [floors] z [planFeatures] i dodaje obrazy numerów
 * sal spoza [numbered], ze wszystkich pięter naraz. Tekst danych i obrazy powstają w [worker], poza wątkiem głównym,
 * a piętro wybiera potem samo [showPlanFloor]. Wymagania: US16.1, NFR-10
 */
internal suspend fun MapStyleTarget.showPlanData(
    floors: Map<String, RoomList>,
    images: MapImages,
    numbered: MutableSet<String>,
    worker: CoroutineContext = Dispatchers.Default,
) {
    val (text, bitmaps) =
        withContext(worker) {
            val numbers = floors.values.flatMap { it.rooms }.filter { it.isLabelled }.mapNotNull { it.number }
            val fresh = numbers.toSet().filterNot(numbered::contains)
            planFeatures(floors).asStyleText() to
                fresh.associateWith { roomNumberBitmap(it, images.colors, images.labelSize) }
        }
    bitmaps.forEach { (number, bitmap) -> image(roomNumberImage(number), bitmap) }
    numbered += bitmaps.keys
    data(source = PLAN_SOURCE, geoJson = text)
}

/** Filtr obiektów rodzaju [kind] piętra [floor]; bez piętra nie przepuszcza niczego. */
internal fun planFilter(floor: String?, kind: String): JsonElement =
    expression(
        "all",
        expression("==", property(FLOOR), floor?.let(::JsonPrimitive) ?: JsonPrimitive(false)),
        expression("==", property(KIND), JsonPrimitive(kind)),
    )

private fun categoryColor(colors: PlanColors): JsonElement {
    val groups =
        RoomCategory.entries.groupBy { category ->
            when (category) {
                RoomCategory.CLASSROOM, RoomCategory.LECTURE_HALL -> colors.classroom
                RoomCategory.CORRIDOR -> colors.corridor
                RoomCategory.ELEVATOR -> colors.elevator
                RoomCategory.TOILET, RoomCategory.ACCESSIBLE_TOILET -> colors.toilet
                RoomCategory.STAIRCASE, RoomCategory.OFFICE, RoomCategory.OTHER -> colors.other
            }
        }
    val cases =
        groups.flatMap { (color, categories) ->
            listOf(JsonArray(categories.map { JsonPrimitive(it.name) }), JsonPrimitive(color))
        }
    return JsonArray(listOf(JsonPrimitive("match"), property(CATEGORY)) + cases + JsonPrimitive(colors.other))
}

private fun part(floor: String, kind: String, category: RoomCategory? = null, image: String? = null): JsonObject =
    buildJsonObject {
        put(key = FLOOR, value = floor)
        put(key = KIND, value = kind)
        category?.let { put(key = CATEGORY, value = it.name) }
        image?.let { put(key = IMAGE, value = it) }
    }
