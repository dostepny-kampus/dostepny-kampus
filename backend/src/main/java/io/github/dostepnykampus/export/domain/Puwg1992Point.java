/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.export.domain;

/**
 * Punkt w PUWG 1992 (EPSG:2180) w metrach (US13.2).
 *
 * <p>W przepisach PL-1992 oś X biegnie na północ, a Y na wschód; kolumny
 * {@code x} i {@code y} eksportu CSV mają odwrotnie, jak pola X i Y warstwy
 * punktów w QGIS, więc nazwy pól mówią kierunek, nie literę osi.</p>
 *
 * @param easting Współrzędna wschodnia, kolumna {@code x} w CSV
 * @param northing Współrzędna północna, kolumna {@code y} w CSV
 * @since 0.1.0
 */
public record Puwg1992Point(double easting, double northing) {
}
