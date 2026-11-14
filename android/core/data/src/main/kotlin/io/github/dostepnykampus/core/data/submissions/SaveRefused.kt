/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

/** Powód, dla którego kolejka nie zapisała zgłoszenia. */
public enum class SaveRefusal {
    /** Zdjęcia nie odpowiadają identyfikatorom w treści. Wymagania: BR-7, BR-8 */
    PHOTOS_MISMATCH,

    /** Serwer ma już zgłoszenie, a telefon nie zna jego wersji. Wymagania: BR-10 */
    UNKNOWN_VERSION,
}

/**
 * Kolejka odmówiła zapisu zgłoszenia z powodu [reason]. Formularz dobiera do niego komunikat.
 *
 * Wymagania: NFR-50
 *
 * @property reason powód odmowy
 * @param message opis dla programisty, bez danych osoby
 */
public class SaveRefusedException(public val reason: SaveRefusal, message: String) : Exception(message)
