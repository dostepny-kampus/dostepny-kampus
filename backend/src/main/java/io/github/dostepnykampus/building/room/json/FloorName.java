/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.json;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nazwa piętra w danych PWr, np. {@code Bud. A1 p 2}.
 *
 * <p>Etykieta pomija przedrostek budynku ({@code Bud.}, {@code Bud}, {@code Budynek} z symbolem).
 * Zapis {@code p N} albo {@code pN} daje „N piętro”, a parter, piwnica, przyziemie i antresola
 * to samo słowo wielką literą. Inna nazwa zostaje bez przedrostka.</p>
 *
 * <p>Wymagania: US16.1, NFR-18</p>
 *
 * @since 0.1.0
 */
final class FloorName {

    /**
     * Przedrostek budynku przed nazwą piętra.
     */
    private static final Pattern BUILDING = Pattern.compile(
        "^bud(?:\\.|ynek)?\\s+[a-z]-?\\d*[a-z]?\\s+", Pattern.CASE_INSENSITIVE
    );

    /**
     * Numer piętra nad parterem.
     */
    private static final Pattern STOREY = Pattern.compile(
        "p\\s*(\\d+)", Pattern.CASE_INSENSITIVE
    );

    /**
     * Nazwy pięter bez numeru, pisane wielką literą.
     */
    private static final Set<String> WORDS = Set.of("parter", "piwnica", "przyziemie", "antresola");

    /**
     * Nazwa z danych PWr.
     */
    private final String name;

    /**
     * Ctor.
     *
     * @param name Nazwa piętra z danych PWr
     */
    FloorName(final String name) {
        this.name = name;
    }

    /**
     * Ujednolicona etykieta piętra.
     *
     * @return Etykieta, np. „2 piętro” albo „Parter”
     */
    String label() {
        final String rest = FloorName.BUILDING.matcher(this.name.strip()).replaceFirst("");
        final Matcher storey = FloorName.STOREY.matcher(rest);
        final String lower = rest.toLowerCase(Locale.ROOT);
        final String label;
        if (storey.matches()) {
            label = String.join(" ", storey.group(1), "piętro");
        } else if (FloorName.WORDS.contains(lower)) {
            label = lower.substring(0, 1).toUpperCase(Locale.ROOT).concat(lower.substring(1));
        } else {
            label = rest;
        }
        return label;
    }
}
