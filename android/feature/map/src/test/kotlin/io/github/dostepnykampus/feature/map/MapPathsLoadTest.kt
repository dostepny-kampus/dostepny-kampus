/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.path.AccessiblePath
import io.github.dostepnykampus.feature.map.campus.NO_SERVER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Ścieżki dostosowane ze źródła w danych mapy V1, na podróbkach źródeł. Wymagania: US1.4, NFR-10 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapPathsLoadTest {
    private val online = MutableStateFlow(true)
    private val answers = ArrayDeque<List<AccessiblePath>>()

    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    /** Wiersz: V1, ścieżki dostosowane ze źródła trafiają do danych mapy jako linie. Wymagania: US1.4 */
    @Test
    fun putsPathsInMapData() =
        runTest(UnconfinedTestDispatcher()) {
            answers += listOf(path("p-1"), path("p-2"))

            assertEquals(listOf("p-1", "p-2"), started().pathIds(), "odcinki ze źródła na mapie")
        }

    /** Wiersz: bez sieci i bez kopii mapa działa dalej, tylko bez ścieżek. Wymagania: NFR-10 */
    @Test
    fun showsMapWithoutPaths() =
        runTest(UnconfinedTestDispatcher()) {
            answers += emptyList<AccessiblePath>()

            assertEquals(emptyList<String>(), started().pathIds(), "mapa bez ścieżek")
        }

    /** Wiersz: po powrocie sieci mapa pyta źródło ścieżek ponownie. Wymagania: NFR-10 */
    @Test
    fun reloadsWhenOnline() =
        runTest(UnconfinedTestDispatcher()) {
            online.value = false
            answers += listOf(path("kopia"))
            answers += listOf(path("serwer"))
            val viewModel = started()

            assertEquals(listOf("kopia"), viewModel.pathIds(), "bez sieci ścieżki z kopii")
            online.value = true
            assertEquals(listOf("serwer"), viewModel.pathIds(), "po powrocie sieci ścieżki z serwera")
        }

    private fun TestScope.started(): CampusMapViewModel =
        mapWithServer(online = online, server = NO_SERVER.copy(paths = { answers.removeFirst() }))

    private fun CampusMapViewModel.pathIds(): List<String> {
        val paths = checkNotNull(uiState.value.mapData).paths
        val features = paths.jsonObject.getValue("features").jsonArray
        return features.map(::pathId)
    }

    private fun pathId(feature: JsonElement): String {
        val properties = feature.jsonObject.getValue("properties").jsonObject
        return properties.getValue("id").jsonPrimitive.content
    }

    private fun path(id: String) =
        AccessiblePath(
            id = id,
            line = listOf(GeoPoint(longitude = 0.2, latitude = 0.2), GeoPoint(longitude = 0.8, latitude = 0.8)),
        )
}
