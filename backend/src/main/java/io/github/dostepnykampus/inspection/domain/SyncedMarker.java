/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Znacznik piętra obejrzanego w zmianach pobieranych przez telefon, także cofnięty.
 *
 * <p>Wymagania: US15.1, US15.2, US11.3, BR-20</p>
 *
 * @param id Identyfikator znacznika
 * @param place Budynek albo piętro znacznika
 * @param created Chwila odbioru znacznika przez serwer
 * @param changed Chwila ostatniej zmiany znacznika
 * @param revoked Czy znacznik cofnięto
 * @param mine Czy znacznik postawiła osoba pobierająca zmiany
 * @since 0.1.0
 */
public record SyncedMarker(
    UUID id, MarkerPlace place, Instant created, Instant changed, boolean revoked, boolean mine
) {
}
