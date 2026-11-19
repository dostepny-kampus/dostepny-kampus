/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.web;

import io.github.dostepnykampus.shared.error.InvalidInput;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Zapytanie sali z parametru {@code q} operacji searchRooms: numer sali i opcjonalnie symbol
 * budynku w dowolnej kolejności, rozdzielone odstępem, przecinkiem albo ukośnikiem; słowa
 * „sala”, „s.” i „pok.” są pomijane. Wielkość liter nie ma znaczenia, a między literą
 * i numerem budynku może stać dywiz, inna kreska, odstęp albo nic, więc „A-1 329”, „329 a1”,
 * „sala 329 A-1” i „A-1/329” szukają sali 329 w budynku A-1.
 *
 * <p>Wymagania: US18.1</p>
 *
 * @since 0.1.0
 */
public final class RoomQuery {

    /**
     * Symbol budynku: litera, numer i opcjonalna litera skrzydła.
     */
    private static final String BUILDING =
        "(?<letter>[A-Za-z])\\s*[-‐‑‒–—―−]?\\s*(?<digits>\\d+)(?<wing>[A-Za-z]?)";

    /**
     * Zapisy zapytania od najbardziej typowego: budynek przed numerem albo sam numer, potem
     * numer przed budynkiem.
     */
    private static final List<Pattern> FORMS = List.of(
        Pattern.compile(String.format("\\s*(?:%s\\s+)?(?<number>\\S+)\\s*", RoomQuery.BUILDING)),
        Pattern.compile(String.format("\\s*(?<number>\\S+)\\s+%s\\s*", RoomQuery.BUILDING))
    );

    /**
     * Słowa przed numerem sali i znaki rozdzielające części, które zapytanie pomija.
     */
    private static final Pattern NOISE = Pattern.compile(
        "(?iu)(?<![\\p{L}\\d])(?:sala|pok\\.|s\\.)(?!\\p{L})|[,/]"
    );

    /**
     * Tekst parametru {@code q}.
     */
    private final String text;

    /**
     * Zapytanie z tekstu; tekst bez numeru sali albo dłuższy niż 50 znaków daje
     * {@link InvalidInput} przy odczycie części.
     *
     * @param text Tekst parametru {@code q}
     */
    public RoomQuery(final String text) {
        this.text = text;
    }

    /**
     * Numer sali albo jego początek.
     *
     * @return Numer bez odstępów na brzegach
     */
    public String number() {
        return this.parts().group("number");
    }

    /**
     * Symbol budynku w zapisie serwera, np. {@code A-1}.
     *
     * @return Symbol; brak, gdy zapytanie ma sam numer
     */
    public Optional<String> building() {
        final Matcher parts = this.parts();
        return Optional.ofNullable(parts.group("letter")).map(
            letter -> String.join(
                "", letter.toUpperCase(Locale.ROOT), "-", parts.group("digits"),
                parts.group("wing").toLowerCase(Locale.ROOT)
            )
        );
    }

    private Matcher parts() {
        final String plain = RoomQuery.NOISE.matcher(this.text).replaceAll(" ");
        return RoomQuery.FORMS.stream()
            .map(form -> form.matcher(plain))
            .filter(parts -> this.text.length() <= 50 && parts.matches())
            .findFirst()
            .orElseThrow(
                () -> new InvalidInput(
                    String.join(
                        " ",
                        "Parametr q to numer sali i opcjonalnie symbol budynku w dowolnej",
                        "kolejności, np. A-1 329 albo 329 A-1, do 50 znaków"
                    )
                )
            );
    }
}
