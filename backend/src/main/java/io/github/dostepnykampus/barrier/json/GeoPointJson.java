/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Punkt GeoJSON, schemat GeoPoint w OpenAPI.
 *
 * @param type Rodzaj geometrii, zawsze Point
 * @param coordinates Długość i szerokość geograficzna, w tej kolejności
 * @since 0.1.0
 */
record GeoPointJson(@Nullable String type, @Nullable List<Double> coordinates) {
}
