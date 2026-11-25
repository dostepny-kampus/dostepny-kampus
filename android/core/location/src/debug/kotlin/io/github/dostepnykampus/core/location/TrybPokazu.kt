/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

/**
 * Tryb pokazu pomiaru C1 z trasy GNSS ułożonej ręcznie, tylko w wersji debug. Każde użycie wymaga zgody (`@OptIn`).
 * TODO(#3806): trasa nagrana przy wejściu A-1 zamiast ułożonej ręcznie.
 *
 * Wymagania: US6.1
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public annotation class TrybPokazu
