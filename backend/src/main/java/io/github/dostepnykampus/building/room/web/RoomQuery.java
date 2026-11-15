/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.web;

import io.github.dostepnykampus.shared.error.InvalidInput;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Zapytanie sali z parametru {@code q} operacji searchRooms: numer sali, a przed nim
 * opcjonalnie symbol budynku w zapisie jak w wyszukiwaniu budynku w aplikacji. Wielkość liter
 * nie ma znaczenia, a między literą i numerem budynku może stać dywiz, inna kreska, odstęp albo
 * nic, więc „A-1 329”, „a1 329” i „A 1 329” szukają sali 329 w budynku A-1.
 *
 * <p>TODO(#1474): odczytywać parametr q w operacji searchRooms.</p>
 *
 * <p>Wymagania: US18.1</p>
 *
 * @since 0.1.0
 */
public final class RoomQuery {

    /**
     * Symbol budynku (grupy 1–3) i numer sali (grupa 4).
     */
    private static final Pattern TEXT = Pattern.compile(
        "\\s*(?:([A-Za-z])\\s*[-‐‑‒–—―−]?\\s*(\\d+)([A-Za-z]?)\\s+)?(\\S+)\\s*"
    );

    /**
     * Tekst parametru {@code q}.
     */
    private final String text;

    /**
     * Zapytanie z tekstu; tekst bez numeru sali daje {@link InvalidInput} przy odczycie części.
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
        return this.parts().group(4);
    }

    /**
     * Symbol budynku w zapisie serwera, np. {@code A-1}.
     *
     * @return Symbol; brak, gdy zapytanie ma sam numer
     */
    public Optional<String> building() {
        final Matcher parts = this.parts();
        return Optional.ofNullable(parts.group(1)).map(
            letter -> String.join(
                "", letter.toUpperCase(Locale.ROOT), "-", parts.group(2),
                parts.group(3).toLowerCase(Locale.ROOT)
            )
        );
    }

    private Matcher parts() {
        final Matcher parts = RoomQuery.TEXT.matcher(this.text);
        if (!parts.matches()) {
            throw new InvalidInput(
                "Parametr q to numer sali, opcjonalnie po symbolu budynku, np. A-1 329"
            );
        }
        return parts;
    }
}
