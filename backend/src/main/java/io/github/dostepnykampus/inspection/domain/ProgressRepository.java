/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.domain;

import java.util.List;

/**
 * Port odczytu postępu inwentaryzacji wszystkich budynków.
 *
 * <p>Wymagania: US11.1, US11.2, US11.3, US11.4, BR-21, BR-24</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface ProgressRepository {

    /**
     * Liczy bieżący postęp dla każdego budynku w kolejności symboli.
     *
     * @param viewer Identyfikator konta czytającego postęp
     * @return Lista postępu budynków
     */
    List<BuildingProgress> read(String viewer);
}
