/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.photos

/**
 * Port zrobienia pojedynczego zdjęcia, bez zależności od modułu funkcji lub sposobu uruchomienia aparatu.
 * TODO(#1450): podłącz port do obsługi zdjęcia w dialogu.
 */
fun interface PhotoCapture {
    /**
     * Zwraca wynik dla [request]. Rezygnacja osoby daje [PhotoResult.Cancelled], błąd aparatu [PhotoResult.Failed].
     * Anulowanie korutyny jest propagowane; nie jest rezygnacją osoby. Bez wyniku [PhotoResult.Ready] adapter
     * usuwa pliki utworzone przez próbę. Gotowe zdjęcie ma identyfikator podany w żądaniu.
     *
     * @param request żądanie zdjęcia
     * @return wynik tej próby
     */
    suspend fun capture(request: PhotoRequest): PhotoResult
}
