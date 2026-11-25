/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port odczytu zgłoszeń; z niego korzystają kontrolery.
 *
 * <p>Wymagania: US8</p>
 *
 * @since 0.1.0
 */
public interface SubmissionFinder {

    /**
     * Szuka zgłoszenia o danym identyfikatorze, w dowolnym statusie.
     *
     * @param id Identyfikator zgłoszenia
     * @return Zgłoszenie albo puste, gdy go nie ma
     */
    Optional<Submission> find(UUID id);

    /**
     * Szuka zgłoszenia, które czytelnik może zobaczyć: przyjęte widzi każdy,
     * pozostałe tylko autor i Weryfikator.
     *
     * <p>Wymagania: BR-5, BR-6</p>
     *
     * @param id Identyfikator zgłoszenia
     * @param reader Osoba, która czyta zgłoszenie
     * @return Zgłoszenie albo puste, gdy go nie ma albo czytelnik go nie widzi
     */
    Optional<Submission> visible(UUID id, Reader reader);

    /**
     * Sprawdza, czy zgłoszenie istnieje i złożyło je inne konto.
     * Zgłoszenie bez zapisanego autora też jest cudze.
     *
     * <p>Wymagania: BR-3</p>
     *
     * @param id Identyfikator zgłoszenia
     * @param account Identyfikator konta, które chce zmienić zgłoszenie
     * @return Prawda dla cudzego zgłoszenia; fałsz dla własnego i dla braku zgłoszenia
     */
    boolean foreign(UUID id, String account);

    /**
     * Sprawdza, czy zwykłą zmianę zgłoszenia przyjętego może złożyć tylko
     * autor, a konto nim nie jest: zgłoszenie istnieje, złożyło je
     * inne konto i nie jest zgłoszeniem szybkim.
     *
     * <p>Wymagania: BR-18</p>
     *
     * @param id Identyfikator zastępowanego zgłoszenia
     * @param account Identyfikator konta, które składa zmianę
     * @return Prawda dla cudzego zgłoszenia innego niż szybkie; fałsz dla
     *  własnego, dla szybkiego i dla braku zgłoszenia
     */
    boolean authorOnly(UUID id, String account);

    /**
     * Wyszukuje zgłoszenia według zapytania.
     *
     * @param query Statusy oraz opcjonalny budynek i piętro
     * @return Zgłoszenia posortowane po chwili utworzenia, potem po identyfikatorze
     */
    List<Submission> list(SubmissionQuery query);

    /**
     * Wyszukuje zgłoszenia jednego autora według zapytania.
     *
     * <p>Wymagania: US8.1</p>
     *
     * @param author Identyfikator konta autora
     * @param query Statusy oraz opcjonalny budynek i piętro
     * @return Zgłoszenia autora posortowane po chwili utworzenia, potem po identyfikatorze
     */
    List<Submission> own(String author, SubmissionQuery query);

    /**
     * Port stron listy zgłoszeń: strona po kursorze, w kolejności identyfikatorów.
     *
     * <p>Wymagania: NFR-34</p>
     *
     * @since 0.1.0
     */
    interface Pages {

        /**
         * Wyszukuje stronę zgłoszeń według zapytania.
         *
         * <p>Wymagania: NFR-34, US1.3</p>
         *
         * @param query Statusy oraz opcjonalny budynek i piętro
         * @param window Kursor i rozmiar strony
         * @return Czytelne zgłoszenia strony i kursor następnej
         */
        Page page(SubmissionQuery query, Window window);

        /**
         * Wyszukuje stronę zgłoszeń jednego autora.
         *
         * <p>Wymagania: NFR-34, US8.1</p>
         *
         * @param author Identyfikator konta autora
         * @param query Statusy oraz opcjonalny budynek i piętro
         * @param window Kursor i rozmiar strony
         * @return Czytelne zgłoszenia strony autora i kursor następnej
         */
        Page own(String author, SubmissionQuery query, Window window);
    }

    /**
     * Okno strony: kursor, po którym strona się zaczyna, i najwięcej zgłoszeń na stronie.
     *
     * @param after Kursor; {@link Page#START} dla pierwszej strony
     * @param limit Najwięcej zgłoszeń na stronie
     * @since 0.1.0
     */
    record Window(UUID after, int limit) {
    }

    /**
     * Strona listy zgłoszeń. Nieczytelne zgłoszenie nie trafia na stronę, ale przesuwa kursor.
     *
     * @param items Czytelne zgłoszenia strony
     * @param last Kursor: identyfikator ostatniego zgłoszenia strony albo kursor wejściowy
     * @param more Czy po kursorze są dalsze zgłoszenia
     * @since 0.1.0
     */
    record Page(List<Submission> items, UUID last, boolean more) {

        /**
         * Kursor pierwszej strony: mniejszy od identyfikatora każdego zgłoszenia.
         */
        public static final UUID START = new UUID(0L, 0L);

        /**
         * Strona z niemodyfikowalną kopią zgłoszeń.
         *
         * @param items Czytelne zgłoszenia strony
         * @param last Kursor ostatniego zgłoszenia strony albo kursor wejściowy
         * @param more Czy po kursorze są dalsze zgłoszenia
         */
        public Page {
            items = List.copyOf(items);
        }
    }
}
