/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

/** Powód, dla którego kolejka nie zapisała zgłoszenia. */
public enum class SaveRefusal {
    /** Zdjęcia nie odpowiadają identyfikatorom w treści. Wymagania: BR-7, BR-8 */
    PHOTOS_MISMATCH,

    /** Serwer ma już zgłoszenie, a telefon nie zna jego wersji. Wymagania: BR-10 */
    UNKNOWN_VERSION,

    /** Zgłoszenie czeka na wybór jednej z dwóch wersji; rozwiązuje to tylko ten wybór. Wymagania: US9.3, BR-10 */
    VERSION_CHOICE_PENDING,

    /** Brak bieżącego konta dla zapisu albo zgłoszenie należy do innego konta. Wymagania: NFR-33 */
    ACCOUNT_CHANGED,
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

/** Odmowa zapisu poprawki zgłoszenia [id], które czeka na wybór wersji. */
internal fun versionChoicePending(id: String): SaveRefusedException =
    SaveRefusedException(SaveRefusal.VERSION_CHOICE_PENDING, "Zgłoszenie $id czeka na wybór wersji (BR-10)")

/** Odmowa zapisu po zmianie konta. */
internal fun queueOwnerChanged(): SaveRefusedException =
    SaveRefusedException(SaveRefusal.ACCOUNT_CHANGED, "Konto nie jest właścicielem zapisu")
