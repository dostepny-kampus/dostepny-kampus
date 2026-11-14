/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Włącznik lokalizacji z systemowego [LocationManager]: stan dostawcy GPS od razu, a potem po każdym systemowym
 * zdarzeniu zmiany dostawców. Stan wynika ze zdarzenia, nie z upływu czasu. Nasłuch trwa, dopóki ktoś
 * zbiera przepływ.
 *
 * Wymagania: NFR-23, NFR-27
 */
internal class GnssLocationSwitch(private val context: Context) : LocationSwitch {
    /** Stan włącznika od teraz, bez powtórzeń tej samej wartości. */
    override fun isEnabled(): Flow<Boolean> =
        callbackFlow {
            val manager = context.getSystemService(LocationManager::class.java)
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        trySend(manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    }
                }
            context.registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
            trySend(manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            awaitClose { context.unregisterReceiver(receiver) }
        }.distinctUntilChanged()
}
