/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.voice

import io.github.dostepnykampus.core.model.submission.BarrierGroup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Grupa barier z odpowiedzi na pytanie dialogu zgłoszenia szybkiego (W5). */
class BarrierGroupSpeechTest {
    @Test
    fun recognizesGroupFromAnswer() {
        val examples =
            mapOf(
                "Za wąskie drzwi." to BarrierGroup.ENTRANCE_AND_DOORS,
                "Domofon jest za wysoko" to BarrierGroup.ENTRANCE_AND_DOORS,
                "Schody przy wejściu do budynku" to BarrierGroup.ENTRANCE_AND_DOORS,
                "schody" to BarrierGroup.VERTICAL_CIRCULATION,
                "Winda nie działa!" to BarrierGroup.VERTICAL_CIRCULATION,
                "Rampa jest za stroma" to BarrierGroup.VERTICAL_CIRCULATION,
                "Inna winda" to BarrierGroup.VERTICAL_CIRCULATION,
                "Wysoki próg w korytarzu" to BarrierGroup.HORIZONTAL_CIRCULATION,
                "Krawężnik bez obniżenia" to BarrierGroup.HORIZONTAL_CIRCULATION,
                "Nie ma toalety dla wózków" to BarrierGroup.TOILETS,
                "Łazienka jest zamknięta" to BarrierGroup.TOILETS,
                "WC" to BarrierGroup.TOILETS,
                "Brak oznakowania sal" to BarrierGroup.ORIENTATION_AND_INFORMATION,
                "Nie ma planu dla niewidomych" to BarrierGroup.ORIENTATION_AND_INFORMATION,
                "Brak pętli indukcyjnej" to BarrierGroup.ORIENTATION_AND_INFORMATION,
                "Nie ma miejsca parkingowego" to BarrierGroup.OTHER,
                "Okienko w dziekanacie za wysoko" to BarrierGroup.OTHER,
                "Coś innego" to BarrierGroup.OTHER,
            )
        examples.forEach { (phrase, group) ->
            assertEquals(group, recognizeBarrierGroup(phrase), "Wypowiedź: $phrase")
        }
    }

    @Test
    fun findsGroupInsideLongerSentence() {
        assertEquals(
            BarrierGroup.VERTICAL_CIRCULATION,
            recognizeBarrierGroup("Zgłaszam, że winda na pierwszym piętrze nie działa, zdjęcie już mam"),
            "Grupa w zdaniu z innymi slotami",
        )
    }

    @Test
    fun emptyWithoutDictionaryWord() {
        listOf("", "  … ", "tak", "nie wiem", "tutaj", "program").forEach { phrase ->
            assertNull(recognizeBarrierGroup(phrase), "Wypowiedź bez słowa ze słownika: $phrase")
        }
    }

    @Test
    fun emptyForTwoGroups() {
        listOf("Ciężkie drzwi i zepsuta winda", "toaleta i parking").forEach { phrase ->
            assertNull(recognizeBarrierGroup(phrase), "Wypowiedź z dwiema grupami: $phrase")
        }
    }
}
