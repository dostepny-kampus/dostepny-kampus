/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.Manifest
import android.content.pm.PackageManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Uprawnienia ze scalonego manifestu aplikacji, razem z tymi, które dokładają biblioteki. */
@RunWith(RobolectricTestRunner::class)
class PermissionsTest {
    private val requested: List<String> =
        RuntimeEnvironment
            .getApplication()
            .run { packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS) }
            .requestedPermissions
            .orEmpty()
            .toList()

    @Test
    fun nfr39_internet_for_osm_tiles() {
        assertTrue(Manifest.permission.INTERNET in requested)
    }

    @Test
    fun nfr27_no_location_before_c1_v4() {
        assertFalse(Manifest.permission.ACCESS_FINE_LOCATION in requested)
        assertFalse(Manifest.permission.ACCESS_COARSE_LOCATION in requested)
        assertFalse(Manifest.permission.ACCESS_BACKGROUND_LOCATION in requested)
    }
}
