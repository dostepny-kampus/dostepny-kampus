/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.photos

/**
 * Żądanie nowego zdjęcia z identyfikatorem nadanym przed uruchomieniem aparatu.
 *
 * @property photoId niepusty identyfikator zdjęcia do zapisania w zgłoszeniu
 * @throws IllegalArgumentException identyfikator jest pusty lub składa się z białych znaków
 */
data class PhotoRequest(val photoId: String) {
    init {
        require(photoId.isNotBlank()) { "Brak identyfikatora zdjęcia" }
    }
}
