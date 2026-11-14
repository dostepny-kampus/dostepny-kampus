/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

/**
 * Tekst treści żądania HTTP z [value] zapisanego przez [strategy].
 *
 * Wymagania: NFR-46
 *
 * @param T typ modelu kontraktu
 * @param strategy serializator modelu
 * @param value model do wysłania
 */
internal fun <T> Json.encodeContract(strategy: SerializationStrategy<T>, value: T): String =
    encodeToString(strategy, value)

/**
 * Model odczytany przez [strategy] z tekstu odpowiedzi HTTP [body].
 *
 * Wymagania: NFR-46
 *
 * @param T typ modelu kontraktu
 * @param strategy deserializator modelu
 * @param body tekst odpowiedzi
 * @throws IllegalArgumentException gdy [body] nie jest poprawnym JSON albo nie pasuje do modelu;
 *   [kotlinx.serialization.SerializationException] jest jej podtypem
 */
internal fun <T> Json.decodeContract(strategy: DeserializationStrategy<T>, body: String): T =
    decodeFromString(strategy, body)
