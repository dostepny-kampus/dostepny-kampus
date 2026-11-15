/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.domain;

import java.util.UUID;

/**
 * Znacznik piętra obejrzanego postawiony przez Zbierającego.
 *
 * <p>Wymagania: US15.1, BR-20</p>
 *
 * @param id Identyfikator znacznika nadany w telefonie
 * @param place Miejsce znacznika: budynek podstawowy albo piętro
 * @param author Identyfikator konta Zbierającego z tokenu
 * @since 0.1.0
 */
public record NewMarker(UUID id, MarkerPlace place, String author) {
}
