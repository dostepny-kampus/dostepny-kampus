/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import io.github.dostepnykampus.core.model.GeoPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val INTERVAL_MILLIS = 1_000L

private const val NANOS_IN_MILLI = 1_000_000L

/**
 * Odczyty z odbiornika GNSS telefonu dla pomiaru na ekranie C1 (US6.1).
 *
 * Bierze tylko dostawcę GPS z systemowego [LocationManager], bez pozycji z sieci, więc każdy odczyt pochodzi z
 * satelitów, a stan satelitów da się czytać z tego samego odbiornika (`GnssStatus`, ADR-0001). Nasłuch trwa, dopóki
 * ktoś zbiera przepływ, i znika razem z nim, więc lokalizacja działa tylko przy otwartym ekranie (NFR-27). Bez
 * zgody na dokładną lokalizację przepływ kończy się [SecurityException]; o zgodę prosi ekran.
 * Odczyt przychodzi co sekundę, bo ekran C1 odświeża dokładność co najwyżej co 1 s (NFR-2).
 */
class GnssPositionFixes(private val context: Context) : PositionSource {
    /** Odczyty od teraz, aż do przerwania zbierania; czas odczytu z zegara monotonicznego telefonu. */
    override fun fixes(): Flow<PositionFix> =
        callbackFlow {
            val manager = context.getSystemService(LocationManager::class.java)
            val permission = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            if (permission == PackageManager.PERMISSION_GRANTED) {
                val listener = LocationListener { location -> trySend(location.toFix()) }
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    INTERVAL_MILLIS,
                    0f,
                    listener,
                    Looper.getMainLooper(),
                )
                awaitClose { manager.removeUpdates(listener) }
            } else {
                close(SecurityException("Brak zgody na dokładną lokalizację"))
            }
        }
}

/** Odczyt bez promienia ufności dostaje dokładność NaN, którą uśrednianie pomija. */
internal fun Location.toFix(): PositionFix =
    PositionFix(
        point = GeoPoint(longitude = longitude, latitude = latitude),
        accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else Double.NaN,
        elapsedMillis = elapsedRealtimeNanos / NANOS_IN_MILLI,
    )
