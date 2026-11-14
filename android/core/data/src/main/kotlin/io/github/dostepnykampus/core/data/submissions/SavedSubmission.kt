/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

/**
 * Wynik zapisu zgłoszenia w kolejce telefonu.
 *
 * @property id identyfikator zapisanego zgłoszenia, zachowywany przy ponowieniu wysyłki
 */
public data class SavedSubmission(val id: String)
