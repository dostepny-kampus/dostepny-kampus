/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.dostepnykampus.feature.map.MapTileHttp
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ConnectException
import javax.inject.Inject

/** Klient używany przez MapLibre dostaje lokalny podkład, z DNS blokującym wyjście do sieci (#609). */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MapTileNetworkDeviceTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject
    lateinit var tiles: MapTileHttp

    @Before
    fun inject() {
        hilt.inject()
    }

    /** Żądanie do prawdziwego adresu OSM przechodzi z blokadą DNS i zwraca przezroczysty lokalny PNG. */
    @Test
    fun tiles_are_local_with_network_blocked() {
        val request = Request.Builder().url("https://tile.openstreetmap.org/0/0/0.png").build()
        tiles.calls.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("image/png", response.body.contentType().toString())
            val bitmap = checkNotNull(BitmapFactory.decodeStream(response.body.byteStream()))
            try {
                assertEquals(256, bitmap.width)
                assertEquals(256, bitmap.height)
                assertEquals(Color.TRANSPARENT, bitmap.getPixel(0, 0))
            } finally {
                bitmap.recycle()
            }
        }
    }

    /** Pozostałe adresy nie mogą wykorzystać lokalnej odpowiedzi kafelków ani otworzyć połączenia. */
    @Test(expected = ConnectException::class)
    fun other_hosts_are_blocked() {
        val request = Request.Builder().url("https://example.org/0/0/0.png").build()
        tiles.calls.newCall(request).execute().close()
    }
}
