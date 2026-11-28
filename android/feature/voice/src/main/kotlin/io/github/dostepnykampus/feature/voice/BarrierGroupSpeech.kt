/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.voice

import io.github.dostepnykampus.core.model.submission.BarrierGroup

/**
 * Rozpoznaje grupę barier w transkrypcji [text], np. „za wąskie drzwi” albo „winda nie działa”.
 * Słowa grupy mogą stać w dowolnym miejscu zdania, bo jedna wypowiedź może wypełnić kilka slotów.
 * Zwraca `null`, gdy wypowiedź nie ma słowa ze słownika albo ma słowa z różnych grup:
 * wtedy dialog pyta o grupę jeszcze raz. „Inne” albo „coś innego” wybiera grupę „inne”
 * tylko jako cała odpowiedź, więc „inna winda” to dalej winda. Dłuższe wyrażenia słownika wygrywają z krótszymi:
 * „schody przy wejściu” to wejście, a same „schody” to komunikacja pionowa.
 *
 * Wymagania: US4.3
 *
 * TODO(#1487): podłączyć do ramki zgłoszenia szybkiego.
 */
fun recognizeBarrierGroup(text: String): BarrierGroup? {
    val phrase = normalizeSpeechText(text)
    if (phrase in STANDALONE_OTHER_ANSWERS) return BarrierGroup.OTHER
    val tokens = phrase.split(' ').filter { it.isNotEmpty() }
    val groups = mutableSetOf<BarrierGroup>()
    var index = 0
    while (index < tokens.size) {
        val synonym = SYNONYMS_LONGEST_FIRST.firstOrNull { it.matchesAt(tokens, index) }
        if (synonym == null) {
            index++
        } else {
            groups += synonym.group
            index += synonym.words.size
        }
    }
    return groups.singleOrNull()
}

/** Wyrażenie słownika: słowa po normalizacji; słowo z `*` na końcu pasuje do każdej odmiany z tym początkiem. */
private class Synonym(val words: List<String>, val group: BarrierGroup) {
    fun matchesAt(tokens: List<String>, start: Int): Boolean =
        start + words.size <= tokens.size &&
            words.indices.all { offset ->
                matchesWord(pattern = words[offset], token = tokens[start + offset])
            }

    private fun matchesWord(pattern: String, token: String): Boolean =
        if (pattern.endsWith('*')) token.startsWith(pattern.dropLast(1)) else token == pattern
}

private val STANDALONE_OTHER_ANSWERS = setOf("inne", "inna", "inny", "cos innego", "inna bariera", "inny problem")

private val DICTIONARY: Map<BarrierGroup, List<String>> =
    mapOf(
        BarrierGroup.ENTRANCE_AND_DOORS to
            listOf(
                "drzwi", "wejsci*", "wejsc", "domofon*", "dzwonek", "dzwonk*", "klamk*",
                "schod* przy wejsci*", "schod* przed wejsci*", "schod* do wejsci*",
                "schod* do budynk*", "schod* przed budynk*",
            ),
        BarrierGroup.VERTICAL_CIRCULATION to
            listOf(
                "schod*", "stopni*", "stopien", "porecz*", "wind*", "pochylni*", "podjazd*",
                "rampa", "rampy", "rampie", "podnosnik*", "platform*",
            ),
        BarrierGroup.HORIZONTAL_CIRCULATION to
            listOf(
                "prog", "progu", "progi", "progiem", "progow", "korytarz*", "przejsci*", "przeszkod*",
                "nawierzchni*", "chodnik*", "kraweznik*", "dziur*", "bruk*", "zwir*",
            ),
        BarrierGroup.TOILETS to
            listOf("toalet*", "wc", "lazien*", "ubikacj*", "sanitariat*", "kibel*", "kibl*"),
        BarrierGroup.ORIENTATION_AND_INFORMATION to
            listOf(
                "oznakowani*", "oznaczen*", "tabliczk*", "drogowskaz*", "napis*", "kontrast*",
                "plan", "planu", "planie", "tyflograf*", "brajl*", "braill*", "indukcyj*",
            ),
        BarrierGroup.OTHER to
            listOf(
                "parking*", "ewakuac*", "okienk*", "lada", "lady", "ladzie", "recepcj*", "dziekanat*",
                "portierni*",
            ),
    )

private val SYNONYMS_LONGEST_FIRST: List<Synonym> =
    DICTIONARY
        .flatMap { (group, phrases) -> phrases.map { Synonym(it.split(' '), group) } }
        .sortedByDescending { it.words.size }
