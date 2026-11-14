/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.inspection;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Znacznik piętra obejrzanego postawiony przez Zbierającego.
 *
 * <p>Wymagania: US15.1, BR-20</p>
 *
 * @param id Identyfikator znacznika nadany w telefonie
 * @param buildingSymbol Symbol budynku znacznika
 * @param floorId Piętro budynku szczegółowego albo {@code null}, gdy znacznik dotyczy
 *  całego budynku podstawowego
 * @since 0.1.0
 */
public record NewMarker(UUID id, String buildingSymbol, @Nullable String floorId) {
}
