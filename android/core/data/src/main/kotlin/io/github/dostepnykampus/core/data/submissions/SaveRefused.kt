/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

/** Powód, dla którego kolejka nie zapisała zgłoszenia. */
enum class SaveRefusal {
    /** Zdjęcia nie odpowiadają identyfikatorom w treści (BR-7, BR-8). */
    PHOTOS_MISMATCH,

    /** Serwer ma już zgłoszenie, a telefon nie zna jego wersji (BR-10). */
    UNKNOWN_VERSION,
}

/**
 * Kolejka odmówiła zapisu zgłoszenia z powodu [reason]. Formularz dobiera do niego komunikat (NFR-50).
 *
 * @property reason powód odmowy
 * @param message opis dla programisty, bez danych osoby
 */
class SaveRefusedException(val reason: SaveRefusal, message: String) : Exception(message)
