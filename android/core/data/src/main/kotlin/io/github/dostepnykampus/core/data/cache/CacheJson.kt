/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.cache

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

private val cacheJson = Json { ignoreUnknownKeys = true }

/**
 * Tekst dokumentu kopii w bazie telefonu z [value] zapisanego przez [strategy].
 *
 * Wymagania: NFR-10, NFR-46
 *
 * @param T typ dokumentu kopii
 * @param strategy serializator dokumentu
 * @param value dokument do zapisania
 */
internal fun <T> encodeCache(strategy: SerializationStrategy<T>, value: T): String =
    cacheJson.encodeToString(strategy, value)

/**
 * Dokument kopii odczytany przez [strategy] z tekstu [text]; nieznane pola są pomijane.
 *
 * Wymagania: NFR-10, NFR-46
 *
 * @param T typ dokumentu kopii
 * @param strategy deserializator dokumentu
 * @param text tekst dokumentu z bazy telefonu
 * @throws IllegalArgumentException gdy [text] nie jest poprawnym JSON albo nie pasuje do dokumentu
 */
internal fun <T> decodeCache(strategy: DeserializationStrategy<T>, text: String): T =
    cacheJson.decodeFromString(strategy, text)
