/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** System GNSS satelity w kolejności napisu na ekranie C1, np. „Satelity: GPS 7, Galileo 5, GLONASS 4” (US6.1). */
enum class GnssSystem {
    /** Amerykański GPS. */
    GPS,

    /** Europejski Galileo. */
    GALILEO,

    /** Rosyjski GLONASS. */
    GLONASS,

    /** Chiński BeiDou. */
    BEIDOU,

    /** Japoński QZSS. */
    QZSS,

    /** Indyjski NavIC (IRNSS). */
    NAVIC,

    /** Satelity wspomagające SBAS, np. EGNOS. */
    SBAS,

    /** System, którego Android nie rozpoznaje. */
    UNKNOWN,
}

/**
 * Satelita widoczny dla odbiornika GNSS telefonu (US6.1, US6.5).
 *
 * @property system system GNSS satelity
 * @property id numer satelity w jego systemie
 * @property azimuthDegrees azymut od północy w stopniach, do mapy nieba
 * @property elevationDegrees wysokość nad horyzontem w stopniach, do mapy nieba
 * @property isUsedInFix czy odbiornik użył satelity do ostatniej pozycji
 */
data class Satellite(
    val system: GnssSystem,
    val id: Int,
    val azimuthDegrees: Double,
    val elevationDegrees: Double,
    val isUsedInFix: Boolean,
)

/**
 * Stan satelitów z odbiornika GNSS telefonu dla ekranu C1 (US6.1, US6.5, NFR-2).
 *
 * Nasłuch `GnssStatus` trwa, dopóki ktoś zbiera przepływ, i znika razem z nim, więc działa tylko przy otwartym ekranie
 * (NFR-27). Bez zgody na dokładną lokalizację przepływ kończy się [SecurityException]; o zgodę prosi ekran.
 */
class GnssSatellites(private val context: Context) : SatelliteSource {
    /** Wszystkie widoczne satelity po każdej zmianie stanu odbiornika. */
    override fun satellites(): Flow<List<Satellite>> =
        callbackFlow {
            val manager = context.getSystemService(LocationManager::class.java)
            val permission = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            if (permission == PackageManager.PERMISSION_GRANTED) {
                val callback =
                    object : GnssStatus.Callback() {
                        override fun onSatelliteStatusChanged(status: GnssStatus) {
                            trySend(status.satellites())
                        }
                    }
                manager.registerGnssStatusCallback(callback, Handler(Looper.getMainLooper()))
                awaitClose { manager.unregisterGnssStatusCallback(callback) }
            } else {
                close(SecurityException("Brak zgody na dokładną lokalizację"))
            }
        }
}

/**
 * Liczba satelitów użytych do pozycji według systemów: tylko systemy z co najmniej jednym, w kolejności [GnssSystem].
 */
fun List<Satellite>.countsInFix(): Map<GnssSystem, Int> {
    val counts = filter { it.isUsedInFix }.groupingBy { it.system }.eachCount()
    return GnssSystem.entries.filter { it in counts }.associateWith { counts.getValue(it) }
}

internal fun gnssSystem(constellation: Int): GnssSystem =
    when (constellation) {
        GnssStatus.CONSTELLATION_GPS -> GnssSystem.GPS
        GnssStatus.CONSTELLATION_GALILEO -> GnssSystem.GALILEO
        GnssStatus.CONSTELLATION_GLONASS -> GnssSystem.GLONASS
        GnssStatus.CONSTELLATION_BEIDOU -> GnssSystem.BEIDOU
        GnssStatus.CONSTELLATION_QZSS -> GnssSystem.QZSS
        GnssStatus.CONSTELLATION_IRNSS -> GnssSystem.NAVIC
        GnssStatus.CONSTELLATION_SBAS -> GnssSystem.SBAS
        else -> GnssSystem.UNKNOWN
    }

internal fun GnssStatus.satellites(): List<Satellite> =
    (0 until satelliteCount).map { index ->
        Satellite(
            system = gnssSystem(getConstellationType(index)),
            id = getSvid(index),
            azimuthDegrees = getAzimuthDegrees(index).toDouble(),
            elevationDegrees = getElevationDegrees(index).toDouble(),
            isUsedInFix = usedInFix(index),
        )
    }
