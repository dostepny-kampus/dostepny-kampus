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

/** Warstwa tła wybranego piętra pod pomieszczeniami, w barwie korytarza. Wymagania: US16.1 */
internal const val PLAN_FLOOR_LAYER = "plan_tlo"

/** Warstwa ścian pomieszczeń wybranego piętra. Wymagania: US16.1 */
internal const val PLAN_WALLS_LAYER = "plan_sciany"

/** Warstwa ściany piętra z obrysu sumy jego pomieszczeń (R11). Wymagania: US16.1 */
internal const val PLAN_OUTLINE_LAYER = "plan_obrys"

/** Warstwa numerów sal wybranego piętra od powiększenia 19 (R12). Wymagania: US16.1 */
internal const val PLAN_NUMBERS_LAYER = "plan_numery"

/** Warstwa znaków windy, toalety dostosowanej i schodów na planie. Wymagania: US16.1 */
internal const val PLAN_SIGNS_LAYER = "plan_znaki"

/** Właściwość znaku planu z identyfikatorem jego pomieszczenia; czyta ją dotknięcie mapy. */
internal const val PLAN_ROOM_ID = "room_id"

/** Bok obrazu znaku na planie w dp: największy rozmiar znaku, od powiększenia 19 (token `mapa.plan.znak_dp`). */
internal const val PLAN_SIGN_DP = 28

private const val SIGN_DP_AT_17 = 20.0
private const val SIGN_DP_AT_18 = 24.0
private const val SIGN_ZOOM_17 = 17.0
private const val SIGN_ZOOM_18 = 18.0
private const val SIGN_ZOOM_19 = 19.0

private val PLAN_SIGN_SIZES =
    listOf(SIGN_ZOOM_17 to SIGN_DP_AT_17, SIGN_ZOOM_18 to SIGN_DP_AT_18, SIGN_ZOOM_19 to PLAN_SIGN_DP.toDouble())

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
private const val WALL_ZOOM_FROM = 17.0
private const val WALL_ZOOM_TO = 20.0
private const val WALL_WIDTH_FROM = 0.75
private const val WALL_WIDTH_TO = 1.5
private val WALL_WIDTHS = listOf(WALL_ZOOM_FROM to WALL_WIDTH_FROM, WALL_ZOOM_TO to WALL_WIDTH_TO)
private const val OUTLINE_WIDTH = 2.4

/**
 * Kolory planu piętra z tokenów `mapa.kolory` jako `#RRGGBB`.
 *
 * @property classroom sala i sala wykładowa (`p_sala`)
 * @property corridor korytarz (`p_korytarz`)
 * @property other klatka schodowa, biuro i reszta (`p_inne`)
 * @property elevator winda (`p_winda`)
 * @property toilet toaleta (`p_wc`)
 * @property wall ściany pomieszczeń (`p_kontur`)
 * @property floorWall ściana piętra, czyli obrys budynku z planem (`pwr_kontur`)
 * @property chosen wypełnienie wyróżnionej sali (`p_wybrana`)
 * @property chosenOutline kontur wyróżnionej sali (`p_wybrana_kontur`)
 */
data class PlanColors(
    val classroom: String,
    val corridor: String,
    val other: String,
    val elevator: String,
    val toilet: String,
    val wall: String,
    val floorWall: String,
    val chosen: String,
    val chosenOutline: String,
)

/**
 * Obiekty planu pięter [floors] (identyfikator piętra → jego plan): pomieszczenie z właściwościami `floor`, `kind`
 * = `room` i `category`, ściana piętra z `floor` i `kind` = `outline`; piętro bez pomieszczeń nie ma ściany. Punkt
 * podpisu pomieszczenia z `labelled` ma `kind` = `number` i nazwę obrazu numeru `image` z [roomNumberImage]. Punkt
 * podpisu windy, toalety dostosowanej i klatki schodowej ma `kind` = `sign`, obraz znaku z [planSignImage]
 * i identyfikator pomieszczenia [PLAN_ROOM_ID].
 *
 * Wymagania: US16.1
 */
internal fun planFeatures(floors: Map<String, RoomList>): JsonElement =
    featureCollection(
        floors.flatMap { (floor, plan) ->
            val walls = plan.floorOutline.takeUnless { it is GeoMultiPolygon && it.polygons.isEmpty() }
            val outline = listOfNotNull(walls?.let { Feature(it.toGeometry(), part(floor, OUTLINE)) })
            val rooms =
                plan.rooms.map { room ->
                    Feature(room.geometry.toGeometry(), part(floor = floor, kind = ROOM, category = room.category, room = room.id))
                }
            val numbers =
                plan.rooms.filter { it.isLabelled }.mapNotNull { room ->
                    room.number?.let {
                        Feature(Point(room.labelPoint.toPosition()), part(floor, NUMBER, image = roomNumberImage(it)))
                    }
                }
            val signs =
                plan.rooms.filter { it.category in PLAN_SIGN_CATEGORIES }.map { room ->
                    val image = planSignImage(room.category)
                    val properties = part(floor = floor, kind = SIGN, image = image, room = room.id)
                    Feature(Point(room.labelPoint.toPosition()), properties)
                }
            outline + rooms + signs + numbers
        },
    )

/**
 * Warstwy planu w kolorach [colors]: tło piętra [floor] w barwie korytarza, pomieszczenia według grupy funkcji, ich
 * ściany grubiejące z powiększeniem, jedna ściana piętra, znaki windy, toalety dostosowanej i schodów oraz numery sal
 * od powiększenia 19; numer albo znak, który zachodzi na inny, znika. Znak rośnie z powiększeniem, a winda i toaleta
 * mają pierwszeństwo przed schodami. Sala [room] ma wyróżnienie z [chosenRoomLayers] nad ścianami. Bez piętra
 * filtry nie przepuszczają niczego.
 *
 * Wymagania: US16.1, NFR-16, NFR-21, US18.1
 */
internal fun planLayers(colors: PlanColors, floor: String? = null, room: String? = null): List<JsonObject> {
    val none = planFilter(floor = floor, kind = ROOM)
    val fill =
        styleLayer(id = PLAN_ROOMS_LAYER, type = "fill", source = PLAN_SOURCE) {
            put(key = FILTER, element = none)
            paint("fill-color" to categoryColor(colors))
        }
    val outline = planFilter(floor = floor, kind = OUTLINE)
    val ground =
        styleLayer(id = PLAN_FLOOR_LAYER, type = "fill", source = PLAN_SOURCE) {
            put(key = FILTER, element = outline)
            paint("fill-color" to JsonPrimitive(colors.corridor))
        }
    val walls =
        styleLayer(id = PLAN_WALLS_LAYER, type = "line", source = PLAN_SOURCE) {
            put(key = FILTER, element = none)
            paint("line-color" to JsonPrimitive(colors.wall), "line-width" to byZoom(WALL_WIDTHS))
        }
    val chosen = chosenRoomLayers(colors = colors, floor = floor, room = room)
    val above =
        listOf(
            lineLayer(
                id = PLAN_OUTLINE_LAYER,
                source = PLAN_SOURCE,
                color = colors.floorWall,
                width = OUTLINE_WIDTH,
                filter = outline,
            ),
            JsonObject(
                symbolLayer(
                    id = PLAN_SIGNS_LAYER,
                    source = PLAN_SOURCE,
                    image = property(IMAGE),
                    "icon-size" to byZoom(PLAN_SIGN_SIZES.map { (zoom, dp) -> zoom to dp / PLAN_SIGN_DP }),
                    "symbol-sort-key" to signOrder(),
                ) +
                    (FILTER to planFilter(floor = floor, kind = SIGN)),
            ),
            JsonObject(
                symbolLayer(id = PLAN_NUMBERS_LAYER, source = PLAN_SOURCE, image = property(IMAGE)) +
                    (FILTER to planFilter(floor = floor, kind = NUMBER)) +
                    ("minzoom" to JsonPrimitive(NUMBERS_MIN_ZOOM)),
            ),
        )
    return listOf(ground, fill, walls) + chosen + above
}

private fun byZoom(stops: List<Pair<Double, Double>>): JsonElement {
    val values = stops.flatMap { (zoom, value) -> listOf(JsonPrimitive(zoom), JsonPrimitive(value)) }
    return JsonArray(listOf(JsonPrimitive("interpolate"), expression("linear"), expression("zoom")) + values)
}

private fun signOrder(): JsonElement =
    expression(
        "case",
        expression("==", property(IMAGE), JsonPrimitive(planSignImage(RoomCategory.STAIRCASE))),
        JsonPrimitive(1),
        JsonPrimitive(0),
    )

/** Nazwa obrazu numeru sali [number]; przedrostek odróżnia go od podpisów budynków. Wymagania: US16.1 */
internal fun roomNumberImage(number: String): String = "sala: $number"

/** Nazwa obrazu znaku grupy funkcji [category] na planie. Wymagania: US16.1 */
internal fun planSignImage(category: RoomCategory): String = "znak planu: ${category.name}"

/**
 * Pokazuje w stylu z [planLayers] piętro [floor] planu już podanego przez [showPlanData]; bez piętra plan znika.
 * Zmienia tylko filtry warstw, więc zmiana piętra nie przelicza danych planu. Wymagania: US16.1, NFR-10
 */
internal fun MapStyleTarget.showPlanFloor(floor: String?) {
    filter(layer = PLAN_FLOOR_LAYER, filter = planFilter(floor = floor, kind = OUTLINE))
    filter(layer = PLAN_ROOMS_LAYER, filter = planFilter(floor = floor, kind = ROOM))
    filter(layer = PLAN_WALLS_LAYER, filter = planFilter(floor = floor, kind = ROOM))
    filter(layer = PLAN_OUTLINE_LAYER, filter = planFilter(floor = floor, kind = OUTLINE))
    filter(layer = PLAN_SIGNS_LAYER, filter = planFilter(floor = floor, kind = SIGN))
    filter(layer = PLAN_NUMBERS_LAYER, filter = planFilter(floor = floor, kind = NUMBER))
}

/**
 * Podaje stylowi z [planLayers] obiekty wszystkich pięter planu [floors] z [planFeatures], a potem dodaje obrazy
 * numerów sal spoza [numbered], ze wszystkich pięter naraz. Tekst danych i obrazy powstają w [worker], poza wątkiem
 * głównym; dane trafiają do stylu przed obrazami, a piętro wybiera potem samo [showPlanFloor].
 * Wymagania: US16.1, NFR-10
 */
internal suspend fun MapStyleTarget.showPlanData(
    floors: Map<String, RoomList>,
    images: MapImages,
    numbered: MutableSet<String>,
    worker: CoroutineContext = Dispatchers.Default,
) {
    data(source = PLAN_SOURCE, geoJson = withContext(worker) { planFeatures(floors).asStyleText() })
    val bitmaps =
        withContext(worker) {
            floors.values
                .asSequence()
                .flatMap { it.rooms }
                .filter { it.isLabelled }
                .mapNotNull { it.number }
                .filterNot(numbered::contains)
                .toSet()
                .associateWith { roomNumberBitmap(it, images.colors, images.labelSize) }
        }
    bitmaps.forEach { (number, bitmap) -> image(roomNumberImage(number), bitmap) }
    numbered += bitmaps.keys
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

private fun part(
    floor: String,
    kind: String,
    category: RoomCategory? = null,
    image: String? = null,
    room: String? = null,
): JsonObject =
    buildJsonObject {
        put(key = FLOOR, value = floor)
        put(key = KIND, value = kind)
        category?.let { put(key = CATEGORY, value = it.name) }
        image?.let { put(key = IMAGE, value = it) }
        room?.let { put(key = PLAN_ROOM_ID, value = it) }
    }
