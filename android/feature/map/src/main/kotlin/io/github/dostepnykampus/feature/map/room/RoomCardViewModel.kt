/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.feature.map.building.orderedEntrances
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Karta sali na V1: sala wybrana w wynikach pola albo na planie, droga do niej z planów pięter w telefonie
 * i wejścia zalecanego budynku oraz krok pokazu drogi. Nowa sala przerywa wczytywanie drogi poprzedniej.
 * TODO(#1481): karta sali na ekranie mapy.
 *
 * Wymagania: US18.1, US18.2, BR-25
 *
 * @param plans plany pięter z kopią w telefonie
 * @param entrances wejścia budynku z serwera z piętrem wejścia
 */
@HiltViewModel
class RoomCardViewModel
    @Inject
    constructor(private val plans: FloorPlans, private val entrances: ServerEntrances) : ViewModel() {
        private val card = MutableStateFlow<RoomCard?>(null)
        private var loading: Job? = null

        /** Otwarta karta; `null`, gdy karta jest zamknięta. */
        val shown: StateFlow<RoomCard?> = card.asStateFlow()

        /** Otwiera kartę sali [found] bez wybranego kroku i wczytuje drogę do niej. */
        fun show(found: FoundRoom) {
            loading?.cancel()
            card.value = RoomCard(found = found, way = null)
            loading =
                viewModelScope.launch {
                    val way = plans.wayTo(found = found, entrances = entrances)
                    card.update { shown -> shown?.takeIf { it.found == found }?.copy(way = way) ?: shown }
                }
        }

        /** Zamyka kartę i kończy pokaz drogi. */
        fun close() {
            loading?.cancel()
            card.value = null
        }

        /** „Pokaż po kolei”: wybiera krok 1. */
        fun start() {
            card.update { it?.started() }
        }

        /** Wybiera krok [index] od zera, dotknięty w karcie albo na planie. */
        fun choose(index: Int) {
            card.update { it?.chosen(index) }
        }

        /** „›”: następny krok. */
        fun next() {
            card.update { it?.next() }
        }

        /** „‹”: poprzedni krok. */
        fun previous() {
            card.update { it?.previous() }
        }
    }

/**
 * Droga do sali [found] z planów pięter budynku, pobranych, gdy telefon nie ma ich kopii, i wejścia zalecanego
 * z [entrances].
 *
 * Wymagania: US18.2, BR-25
 */
internal suspend fun FloorPlans.wayTo(found: FoundRoom, entrances: ServerEntrances): RoomWay {
    val symbol = found.building
    val floors = (floors(symbol) ?: if (download(symbol)) floors(symbol) else null)?.floors.orEmpty()
    val rooms = floors.associate { floor -> floor.id to rooms(symbol, floor.id)?.rooms.orEmpty() }
    // TODO(#3334): wejście zalecane bez przyjętych barier przy wejściach.
    val recommended = orderedEntrances(entrances.entrances(symbol)).firstOrNull { it.isRecommended }
    return roomWay(found = found, entrance = recommended?.entrance, floors = floors, rooms = { rooms[it].orEmpty() })
}
