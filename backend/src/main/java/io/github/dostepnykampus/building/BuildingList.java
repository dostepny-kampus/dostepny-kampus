/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import java.util.List;

/**
 * Lista budynków z atrybucją konturów, zgodna ze schematem BuildingList.
 *
 * @param source Atrybucja konturów budynków
 * @param buildings Budynki posortowane według symbolu
 * @since 0.1.0
 */
public record BuildingList(String source, List<Building> buildings) {
}
