/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import android.content.Context
import io.github.dostepnykampus.core.model.GeoPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject

private const val ROUTE_ASSET = "trasa-pokazu.json"
private const val TICK_MILLIS = 1_000L

/**
 * Odczyt trasy pokazu: położenie z dokładnością w chwili [offsetMillis] od startu trasy.
 *
 * @property offsetMillis czas od startu trasy
 * @property point punkt w WGS84
 * @property accuracyMeters promień ufności w metrach
 */
@TrybPokazu
public data class DemoFix(val offsetMillis: Long, val point: GeoPoint, val accuracyMeters: Double)

/**
 * Satelita trasy pokazu, użyty do pozycji od chwili [usedFromMillis] po starcie trasy.
 *
 * @property satellite satelita z położeniem na niebie; [Satellite.isUsedInFix] wyznacza [usedFromMillis]
 * @property usedFromMillis czas od startu trasy, od którego odbiornik używa satelity
 */
@TrybPokazu
public data class DemoSatellite(val satellite: Satellite, val usedFromMillis: Long)

/**
 * Trasa GNSS pokazu z pliku `trasa-pokazu.json` w zasobach debug. Źródła odtwarzają ją w czasie rzeczywistym
 * na zegarze [ElapsedClock]: odczyty powtarzają się w kółko, a satelity od startu zbierania kolejno wchodzą do pozycji.
 *
 * Wymagania: US6.1, US6.5
 *
 * @property fixes odczyty od najwcześniejszego
 * @property satellites widoczne satelity
 */
@TrybPokazu
public class DemoRoute(public val fixes: List<DemoFix>, public val satellites: List<DemoSatellite>) {
    /** Odczyty trasy ze znacznikiem czasu [clock] liczonym od początku zbierania. */
    public fun positions(clock: ElapsedClock): PositionSource =
        PositionSource {
            flow {
                val period = fixes.last().offsetMillis + TICK_MILLIS
                var start = clock.nowMillis()
                while (true) {
                    fixes.forEach { fix ->
                        val at = start + fix.offsetMillis
                        delay((at - clock.nowMillis()).coerceAtLeast(0))
                        emit(PositionFix(point = fix.point, accuracyMeters = fix.accuracyMeters, elapsedMillis = at))
                    }
                    start += period
                }
            }
        }

    /** Stan satelitów co sekundę od początku zbierania. */
    public fun satellites(clock: ElapsedClock): SatelliteSource =
        SatelliteSource {
            flow {
                val start = clock.nowMillis()
                while (true) {
                    emit(visibleAt(clock.nowMillis() - start))
                    delay(TICK_MILLIS)
                }
            }
        }

    /** Satelity w chwili [offsetMillis] od startu trasy. */
    public fun visibleAt(offsetMillis: Long): List<Satellite> =
        satellites.map { it.satellite.copy(isUsedInFix = offsetMillis >= it.usedFromMillis) }

    /** Odczyt trasy z zasobów. */
    public companion object {
        /** Trasa z pliku zasobów debug aplikacji [context]. */
        public fun load(context: Context): DemoRoute =
            parse(context.assets.open(ROUTE_ASSET).bufferedReader().use { it.readText() })

        /** Trasa z tekstu JSON [text]; błędny tekst kończy się [org.json.JSONException]. */
        public fun parse(text: String): DemoRoute {
            val json = JSONObject(text)
            return DemoRoute(
                fixes = json.getJSONArray("fixes").objects().map(::fixOf),
                satellites = json.getJSONArray("satellites").objects().map(::satelliteOf),
            )
        }

        private fun fixOf(item: JSONObject) =
            DemoFix(
                offsetMillis = item.getLong("ms"),
                point = GeoPoint(latitude = item.getDouble("lat"), longitude = item.getDouble("lon")),
                accuracyMeters = item.getDouble("accuracy"),
            )

        private fun satelliteOf(item: JSONObject) =
            DemoSatellite(
                satellite =
                    Satellite(
                        system = GnssSystem.valueOf(item.getString("system")),
                        id = item.getInt("id"),
                        azimuthDegrees = item.getDouble("azimuth"),
                        elevationDegrees = item.getDouble("elevation"),
                        isUsedInFix = false,
                    ),
                usedFromMillis = item.getLong("usedFromMs"),
            )

        private fun JSONArray.objects(): List<JSONObject> = List(size = length()) { index -> getJSONObject(index) }
    }
}
