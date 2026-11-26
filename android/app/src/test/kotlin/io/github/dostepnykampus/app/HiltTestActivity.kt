/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.AndroidEntryPoint
import org.junit.rules.ExternalResource
import org.robolectric.Shadows.shadowOf

/**
 * Pusta aktywność z grafem Hilt dla testu, który sam składa ekran: ekrany z `hiltViewModel()` potrzebują
 * aktywności z `@AndroidEntryPoint`, a zwykła `ComponentActivity` jej nie ma.
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()

/**
 * Rejestruje [HiltTestActivity] w menedżerze pakietów Robolectric, zanim reguła Compose ją uruchomi; aplikacja
 * nie ma jej w manifeście. Stoi między `HiltAndroidRule` a regułą Compose.
 */
class HiltTestActivityRule : ExternalResource() {
    override fun before() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, HiltTestActivity::class.java))
    }
}
