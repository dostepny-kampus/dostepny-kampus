/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.data.map.MapListChoice
import kotlinx.coroutines.flow.MutableStateFlow

/** Wybór „Mapa” albo „Lista budynków” w pamięci testu; na start osoba nic nie wybrała. */
internal class TestMapListChoice : MapListChoice {
    override val savedChoice = MutableStateFlow<Boolean?>(null)

    override suspend fun choose(isList: Boolean) {
        savedChoice.value = isList
    }
}
