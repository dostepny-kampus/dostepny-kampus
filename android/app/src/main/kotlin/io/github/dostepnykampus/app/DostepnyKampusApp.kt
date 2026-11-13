/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Aplikacja z grafem zależności Hilt na cały proces: repozytoria `core:data` i dostawca ról. */
@HiltAndroidApp
class DostepnyKampusApp : Application()
