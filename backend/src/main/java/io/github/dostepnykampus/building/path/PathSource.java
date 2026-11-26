/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path;

import java.util.List;

/**
 * Odcinki ścieżek dostosowanych z warstwy usługi mapy kampusu PWr.
 *
 * <p>Wymagania: US1.4, NFR-40</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface PathSource {

    /**
     * Odcinki warstwy jako linie GeoJSON w WGS 84. Obiekt bez identyfikatora albo bez linii
     * z co najmniej dwoma punktami jest pomijany.
     *
     * @param service Adres usługi FeatureServer
     * @param layer Numer warstwy
     * @return Odcinki w kolejności usługi
     */
    List<AccessiblePath> paths(String service, int layer);
}
