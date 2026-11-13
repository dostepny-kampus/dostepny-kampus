/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.photos

import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft

/** Wynik próby zrobienia zdjęcia, przypisany do jej żądania. */
sealed interface PhotoResult {
    /** Żądanie, którego dotyczy wynik, także przy rezygnacji lub błędzie. */
    val request: PhotoRequest

    /**
     * Gotowy JPEG bez metadanych w pamięci aplikacji; plik przechodzi pod opiekę wywołującego.
     *
     * @property request żądanie zdjęcia
     * @property photo plik zdjęcia o identyfikatorze z [request]
     * @throws IllegalArgumentException wynik zawiera zdjęcie z innego żądania
     */
    data class Ready(override val request: PhotoRequest, val photo: PendingPhotoDraft) : PhotoResult {
        init {
            require(photo.photoId == request.photoId) { "Zdjęcie nie odpowiada żądaniu" }
        }
    }

    /** Rezygnacja osoby robiącej zdjęcie; nie przekazuje pliku. */
    data class Cancelled(override val request: PhotoRequest) : PhotoResult

    /** Nieudana próba bez pliku do zachowania. */
    data class Failed(override val request: PhotoRequest, val reason: PhotoFailure) : PhotoResult
}

/** Powód nieudanej próby, bez tekstu przeznaczonego dla interfejsu. */
enum class PhotoFailure {
    /** Telefon nie ma dostępnej aplikacji aparatu. */
    UNAVAILABLE,

    /** Nie udało się odczytać, przekodować lub zapisać zdjęcia. */
    PROCESSING,
}
