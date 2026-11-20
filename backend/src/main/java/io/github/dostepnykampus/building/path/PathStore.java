/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path;

import java.util.List;

/**
 * Zapis odcinków ścieżek dostosowanych z importu.
 *
 * <p>Wymagania: US1.4, NFR-40</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface PathStore {

    /**
     * Zastępuje wszystkie zapisane odcinki podanymi, w jednej transakcji: odczyt widzi albo
     * stare odcinki, albo nowe.
     *
     * @param paths Odcinki z linią GeoJSON w WGS 84 i niepowtarzalnym identyfikatorem
     */
    void replace(List<AccessiblePath> paths);
}
