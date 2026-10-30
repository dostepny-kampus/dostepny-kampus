/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.network

import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NetworkStatusTest {
    private fun internet(vararg capabilities: Int) = hasInternet { it in capabilities }

    // US9.1, NFR-10: internet jest tylko wtedy, gdy sieć go ma i system go sprawdził.
    @Test
    fun us9_1_validated_internet() {
        val cases =
            listOf(
                internet(NET_CAPABILITY_INTERNET, NET_CAPABILITY_VALIDATED),
                internet(NET_CAPABILITY_INTERNET, NET_CAPABILITY_VALIDATED, NET_CAPABILITY_NOT_METERED),
                internet(NET_CAPABILITY_INTERNET),
                internet(NET_CAPABILITY_VALIDATED),
                internet(),
            )
        assertEquals(listOf(true, true, false, false, false), cases)
    }
}
