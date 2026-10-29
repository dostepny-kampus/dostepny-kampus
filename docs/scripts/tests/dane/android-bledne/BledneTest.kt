/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package przyklad

/** Błędne znaczniki scenariuszy z pliku `kampus.feature`: żaden nie pokrywa scenariusza. */
class BledneTest {
    // Scenariusz: kampus.feature: Wyszukanie "A-1 329"
    @Test
    fun zlaNazwa() = Unit

    // Scenariusz: mapa.feature: Pełny test
    @Test
    fun zlyPlik() = Unit

    // Scenariusz: kampus.feature: Pełny test

    @Test
    fun pustaLinia() = Unit

    // Scenariusz: kampus.feature: Pełny test
    fun pomocnicza() = Unit

    // Scenariusz: kampus.feature: Pełny test
    /** Opis testu stoi nad znacznikiem, nie pod nim. */
    @Test
    fun opisPodZnacznikiem() = Unit

    // Scenariusz (czesciowo): kampus.feature: Tylko częściowo
    @Test
    fun literowka() = Unit

    // Scenariusz: kampus.feature: Zgłoszenie „Brak windy
    // przy wejściu głównym” trafia do kolejki zgłoszeń
    @Test
    fun ciagDalszyBezWciecia() = Unit

    // Scenariusz: kampus.feature:
    @Test
    fun bezNazwy() = Unit
}
