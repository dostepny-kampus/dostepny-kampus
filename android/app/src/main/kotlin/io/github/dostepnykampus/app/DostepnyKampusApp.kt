/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import io.github.dostepnykampus.app.di.ImageLoaderEntryPoint
import timber.log.Timber

/**
 * Aplikacja z grafem zależności Hilt na cały proces: repozytoria `core:data` i dostawca ról. Domyślny
 * [ImageLoader] obrazów w całej aplikacji to ten z grafu. Przy starcie sadzi drzewo logów: w wersji debug zapisuje
 * wszystko od DEBUG, w wersji release tylko WARN i ERROR ([ReleaseLogTree]).
 *
 * Wymagania: NFR-57
 */
@HiltAndroidApp
class DostepnyKampusApp :
    Application(),
    SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ReleaseLogTree())
        Timber.i("Start aplikacji, wersja %s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        EntryPointAccessors.fromApplication(this, ImageLoaderEntryPoint::class.java).imageLoader()
}
