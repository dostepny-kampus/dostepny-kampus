/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Czy telefon ma teraz internet; bez niego ekrany pokazują linię „Bez internetu” (PW-4). Wymagania: US9.1, NFR-10 */
public fun interface NetworkStatus {
    /** Prawda, gdy jest internet. Pierwsza wartość to stan bieżący, kolejne przychodzą tylko przy zmianie. */
    public fun isOnline(): Flow<Boolean>
}

/**
 * Czy sieć daje internet: ma go mieć według zdolności sieci i system musi go sprawdzić. Samo połączenie
 * z Wi-Fi bez dostępu, np. przed zalogowaniem w sieci hotelowej, nie jest internetem.
 *
 * @param hasCapability czy sieć ma zdolność, np. [NetworkCapabilities.NET_CAPABILITY_INTERNET]
 */
internal fun hasInternet(hasCapability: (Int) -> Boolean): Boolean =
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

/**
 * [NetworkStatus] z domyślnej sieci telefonu; obserwacja kończy się razem z odbiorcą przepływu. Bez domyślnej sieci
 * system nie wywoła żadnej metody, więc stan bieżący idzie od razu.
 */
public class ConnectivityNetworkStatus(private val connectivity: ConnectivityManager) : NetworkStatus {
    override fun isOnline(): Flow<Boolean> =
        callbackFlow {
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                        trySend(hasInternet(capabilities::hasCapability))
                    }

                    override fun onLost(network: Network) {
                        trySend(false)
                    }
                }
            val current = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
            trySend(current?.let { hasInternet(it::hasCapability) } == true)
            connectivity.registerDefaultNetworkCallback(callback)
            awaitClose { connectivity.unregisterNetworkCallback(callback) }
        }.distinctUntilChanged()
}
