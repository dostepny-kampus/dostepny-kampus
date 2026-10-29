/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package przyklad

/** Poprawne znaczniki scenariuszy z pliku `kampus.feature` (plik nie jest kompilowany). */
class PoprawneTest {
    // Scenariusz: kampus.feature: Pełny test
    @Test
    fun pelny() = Unit

    // Scenariusz: kampus.feature: Wyszukanie „A-1 329”
    // Scenariusz (częściowo): kampus.feature: Częściowo i w całości
    @ParameterizedTest
    @ValueSource(
        strings = ["A-1 329", "a1 329"],
    )
    fun wyszukanie(zapis: String) = Unit

    // Scenariusz (częściowo): kampus.feature: Tylko częściowo
    @Test fun czesciowo() = Unit

    // Scenariusz: kampus.feature: Częściowo i w całości
    @Tag("szybki")
    @Test
    internal fun calosc() = Unit

    // Scenariusz: kampus.feature:
    //   Po wysłaniu „Wstecz” wraca do ekranu, z którego zaczęto zgłoszenie
    @Test
    fun nazwaWNastepnejLinii() = Unit

    // Scenariusz: kampus.feature: Zgłoszenie „Brak windy
    //   przy wejściu głównym” trafia
    //   do kolejki zgłoszeń
    @Test
    fun nazwaPodzielonaWCudzyslowie() = Unit
}
