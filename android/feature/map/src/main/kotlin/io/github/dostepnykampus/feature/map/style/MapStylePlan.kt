/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.designsystem.MapaPlan
import io.github.dostepnykampus.core.designsystem.ZoomStop
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

/** Id sali dostępnej przez dotyk planu. Wymagania: US18.1 */
internal const val PLAN_TAPPABLE_ROOM = "tap_room"

/** Warstwa numerów sal wybranego piętra od powiększenia 19 (R12). Wymagania: US16.1 */
internal const val PLAN_NUMBERS_LAYER = "plan_numery"

/** Warstwa znaków windy, toalety dostosowanej i schodów na planie. Wymagania: US16.1 */
internal const val PLAN_SIGNS_LAYER = "plan_znaki"

/** Właściwość znaku planu z identyfikatorem jego pomieszczenia; czyta ją dotknięcie mapy. */
internal const val PLAN_ROOM_ID = "room_id"

/** Bok obrazu znaku na planie w dp: największy rozmiar znaku z tokenu `mapa.plan.znak_dp`. */
internal val PLAN_SIGN_DP: Double = MapaPlan.znakDp.maxOf(ZoomStop::dp)

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
internal const val PLAN_ROOM_NUMBER_ZOOM = 19.0

/** Rodzaj obiektów planu, które każda warstwa planu pokazuje na wybranym piętrze. Wymagania: US16.1 */
internal val PLAN_LAYER_KINDS: List<Pair<String, String>> =
    listOf(
        PLAN_FLOOR_LAYER to OUTLINE,
        PLAN_ROOMS_LAYER to ROOM,
        PLAN_WALLS_LAYER to ROOM,
        PLAN_OUTLINE_LAYER to OUTLINE,
        PLAN_SIGNS_LAYER to SIGN,
        PLAN_NUMBERS_LAYER to NUMBER,
    )

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
                    val roomProperties = part(floor = floor, kind = ROOM, category = room.category, room = room.id)
                    val id = room.id.takeIf { room.isLabelled && !room.number.isNullOrBlank() }
                    val properties = id?.let { JsonObject(roomProperties + (PLAN_TAPPABLE_ROOM to JsonPrimitive(it))) }
                    Feature(room.geometry.toGeometry(), properties ?: roomProperties)
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
    val filters = PLAN_LAYER_KINDS.associate { (layer, kind) -> layer to planFilter(floor = floor, kind = kind) }
    val none = filters.getValue(PLAN_ROOMS_LAYER)
    val fill =
        styleLayer(id = PLAN_ROOMS_LAYER, type = "fill", source = PLAN_SOURCE) {
            put(key = FILTER, element = none)
            paint("fill-color" to categoryColor(colors))
        }
    val ground =
        styleLayer(id = PLAN_FLOOR_LAYER, type = "fill", source = PLAN_SOURCE) {
            put(key = FILTER, element = filters.getValue(PLAN_FLOOR_LAYER))
            paint("fill-color" to JsonPrimitive(colors.corridor))
        }
    val walls =
        styleLayer(id = PLAN_WALLS_LAYER, type = "line", source = PLAN_SOURCE) {
            put(key = FILTER, element = none)
            paint("line-color" to JsonPrimitive(colors.wall), "line-width" to byZoom(MapaPlan.scianyDp))
        }
    val chosen = chosenRoomLayers(colors = colors, floor = floor, room = room)
    val above =
        listOf(
            lineLayer(
                id = PLAN_OUTLINE_LAYER,
                source = PLAN_SOURCE,
                color = colors.floorWall,
                width = MapaPlan.obrysDp,
                filter = filters.getValue(PLAN_OUTLINE_LAYER),
            ),
            JsonObject(
                symbolLayer(
                    id = PLAN_SIGNS_LAYER,
                    source = PLAN_SOURCE,
                    image = property(IMAGE),
                    "icon-size" to byZoom(MapaPlan.znakDp.map { it.copy(dp = it.dp / PLAN_SIGN_DP) }),
                    "symbol-sort-key" to signOrder(),
                ) +
                    (FILTER to filters.getValue(PLAN_SIGNS_LAYER)),
            ),
            JsonObject(
                symbolLayer(id = PLAN_NUMBERS_LAYER, source = PLAN_SOURCE, image = property(IMAGE)) +
                    (FILTER to filters.getValue(PLAN_NUMBERS_LAYER)) +
                    ("minzoom" to JsonPrimitive(PLAN_ROOM_NUMBER_ZOOM)),
            ),
        )
    return listOf(ground, fill, walls) + chosen + above
}

private fun byZoom(stops: List<ZoomStop>): JsonElement {
    val values = stops.flatMap { listOf(JsonPrimitive(it.zoom), JsonPrimitive(it.dp)) }
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
    PLAN_LAYER_KINDS.forEach { (layer, kind) -> filter(layer = layer, filter = planFilter(floor = floor, kind = kind)) }
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
    data(source = PLAN_SOURCE, geoJson = withContext(worker) { geoJsonText(PLAN_SOURCE) { planFeatures(floors) } })
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
