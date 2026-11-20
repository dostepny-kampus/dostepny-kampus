/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.application;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Usługa mapy kampusu PWr {@code dk.campus-map} z pliku pwr-campus-map.yaml: jedna usługa
 * FeatureServer z warstwami całego kampusu, niezależna od planów budynków. Plik zawiera tylko
 * adres i numery warstw, bez danych PWr.
 *
 * <p>Wymagania: NFR-40, NFR-47</p>
 *
 * @param service Adres usługi FeatureServer bez końcowego ukośnika
 * @param layers Numery używanych warstw usługi
 * @since 0.1.0
 */
@ConfigurationProperties("dk.campus-map")
public record CampusMapSettings(URI service, Layers layers) {

    /**
     * Numery warstw usługi mapy kampusu.
     *
     * @param entrances Warstwa wejść do budynków z uwagami i szerokością drzwi
     * @since 0.1.0
     */
    public record Layers(int entrances) {
    }
}
