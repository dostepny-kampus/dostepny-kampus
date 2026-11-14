/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Funkcja pomieszczenia z planu PWr. Pasuje do grupy dokładną nazwą albo początkiem nazwy, bez
 * względu na wielkość liter i spacje na brzegach; funkcja spoza tabeli należy do grupy OTHER.
 *
 * <p>TODO(#1918): zapisywać grupę przy imporcie planu.</p>
 *
 * <p>Wymagania: US16.1, US18.1</p>
 *
 * @since 0.1.0
 */
public final class PwrFunction {

    /**
     * Funkcje PWr o dokładnej nazwie.
     */
    private static final Map<String, RoomCategory> EXACT = Map.ofEntries(
        Map.entry("sala dydaktyczna", RoomCategory.CLASSROOM),
        Map.entry("laboratorium", RoomCategory.CLASSROOM),
        Map.entry("pracownia komputerowa", RoomCategory.CLASSROOM),
        Map.entry("sala wykładowa", RoomCategory.LECTURE_HALL),
        Map.entry("aula", RoomCategory.LECTURE_HALL),
        Map.entry("korytarz", RoomCategory.CORRIDOR),
        Map.entry("winda", RoomCategory.ELEVATOR),
        Map.entry("toaleta dla ozn", RoomCategory.ACCESSIBLE_TOILET),
        Map.entry("toaleta dla osób z niepełnosprawnością", RoomCategory.ACCESSIBLE_TOILET),
        Map.entry("gabinet", RoomCategory.OFFICE),
        Map.entry("pomieszczenie administracyjne", RoomCategory.OFFICE)
    );

    /**
     * Początki nazw funkcji PWr, sprawdzane po nazwach dokładnych.
     */
    private static final Map<String, RoomCategory> PREFIX = Map.of(
        "klatka schodowa", RoomCategory.STAIRCASE,
        "schody", RoomCategory.STAIRCASE,
        "toaleta", RoomCategory.TOILET,
        "wc", RoomCategory.TOILET,
        "dziekanat", RoomCategory.OFFICE,
        "biuro", RoomCategory.OFFICE
    );

    /**
     * Nazwa funkcji z planu PWr.
     */
    private final String name;

    /**
     * Tworzy funkcję z planu PWr.
     *
     * @param name Nazwa funkcji z planu PWr
     */
    public PwrFunction(final String name) {
        this.name = name;
    }

    /**
     * Grupa funkcji.
     *
     * @return Grupa funkcji, OTHER dla funkcji spoza tabeli
     */
    public RoomCategory category() {
        final String key = this.name.strip().toLowerCase(Locale.ROOT);
        return Optional.ofNullable(PwrFunction.EXACT.get(key)).orElseGet(
            () -> PwrFunction.PREFIX.entrySet().stream()
                .filter(entry -> key.startsWith(entry.getKey()))
                .map(Map.Entry::getValue).findFirst().orElse(RoomCategory.OTHER)
        );
    }
}
