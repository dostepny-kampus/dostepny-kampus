/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.domain;

/**
 * Wejście z oznaczeniem zmienionym na obchodzie razem z budynkiem, do którego prowadzi.
 *
 * <p>Wymagania: US17.2</p>
 *
 * @param building Symbol budynku
 * @param entrance Wejście
 * @since 0.1.0
 */
public record MarkedEntrance(String building, Entrance entrance) {
}
