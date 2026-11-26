/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.api;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Znacznik piętra obejrzanego zmieniony od kursora, także cofnięty.
 *
 * <p>Wymagania: US15.1, US15.2, US11.3, BR-20</p>
 *
 * @param id Identyfikator znacznika
 * @param buildingSymbol Symbol budynku
 * @param floorId Identyfikator piętra albo {@code null} przy znaczniku całego budynku
 * @param created Chwila odbioru znacznika przez serwer
 * @param changed Chwila ostatniej zmiany znacznika
 * @param revoked Czy znacznik cofnięto
 * @param mine Czy znacznik postawiła osoba pobierająca zmiany
 * @since 0.1.0
 */
public record ChangedMarker(
    UUID id, String buildingSymbol, @Nullable String floorId, Instant created, Instant changed,
    boolean revoked, boolean mine
) {
}
