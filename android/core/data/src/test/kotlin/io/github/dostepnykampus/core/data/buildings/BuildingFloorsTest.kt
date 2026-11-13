/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import io.github.dostepnykampus.core.model.Floor
import io.github.dostepnykampus.core.model.FloorList
import io.github.dostepnykampus.core.network.ApiResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.IOException

/** Piętra budynku z serwera z pamięcią ostatniej udanej odpowiedzi. Wymagania: US7.2, BR-G1 */
class BuildingFloorsTest {
    @Test
    fun `us7_2 piętra z serwera`() =
        runBlocking {
            val floors = ServerFloorsRepository { ApiResult.Success(LIST) }

            assertEquals(LIST, floors.floors("C-13"))
        }

    @Test
    fun `br_g1 bez sieci ostatnie`() =
        runBlocking {
            val answers = ArrayDeque(listOf(ApiResult.Success(LIST), OFFLINE, OFFLINE, ApiResult.NotFound))
            val asked = mutableListOf<String>()
            val floors =
                ServerFloorsRepository { symbol ->
                    asked += symbol
                    answers.removeFirst()
                }

            assertEquals(LIST, floors.floors("C-13"))
            assertEquals(LIST, floors.floors("C-13"))
            assertNull(floors.floors("A-1"))
            assertNull(floors.floors("X-9"))
            assertEquals(listOf("C-13", "C-13", "A-1", "X-9"), asked)
        }

    private companion object {
        val PARTER = Floor(id = "c13-0", label = "Parter", order = 0)
        val LIST = FloorList(source = "Politechnika Wrocławska", floors = listOf(PARTER))
        val OFFLINE = ApiResult.NetworkError(IOException("brak sieci"))
    }
}
