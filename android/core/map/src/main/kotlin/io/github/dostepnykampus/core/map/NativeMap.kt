/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.LibraryLoader
import kotlin.coroutines.CoroutineContext

private const val LIBRARY = "maplibre"

/**
 * `true`, gdy biblioteka natywna MapLibre jest wczytana. Wczytuje ją w [context] poza wątkiem głównym, bo szukanie
 * biblioteki czyta dysk. Do tej chwili i bez biblioteki (test JVM) mapa nie powstaje.
 *
 * Wymagania: NFR-11
 */
@Composable
public fun rememberNativeMap(context: CoroutineContext = nativeLoadContext()): Boolean {
    val isLoaded by produceState(initialValue = false) { value = withContext(context) { loadNativeMap() } }
    return isLoaded
}

private fun nativeLoadContext(context: CoroutineContext = Dispatchers.IO): CoroutineContext = context

/**
 * Wczytuje bibliotekę natywną MapLibre; `false`, gdy jej nie ma. Po udanym wczytaniu [LibraryLoader] MapLibre uznaje
 * ją za wczytaną, więc `MapView` na wątku głównym już jej nie szuka.
 */
internal fun loadNativeMap(): Boolean {
    val isLoaded = runCatching { System.loadLibrary(LIBRARY) }.isSuccess
    if (isLoaded) LibraryLoader.load()
    return isLoaded
}
