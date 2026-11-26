/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.sync.application;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Znacznik piętra w pobraniu zmian.
 *
 * <p>Wymagania: US15.1, US15.2, US11.3</p>
 *
 * @param id Identyfikator znacznika
 * @param buildingSymbol Symbol budynku
 * @param floorId Identyfikator piętra albo {@code null} przy znaczniku całego budynku
 * @param created Chwila odbioru znacznika przez serwer
 * @param revoked Czy znacznik cofnięto
 * @param mine Czy znacznik postawiła osoba pobierająca zmiany
 * @since 0.1.0
 */
public record PulledMarker(
    UUID id, String buildingSymbol, @Nullable String floorId, Instant created, boolean revoked,
    boolean mine
) {
}
