/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.path

import io.github.dostepnykampus.core.model.GeoPoint

/**
 * Odcinek ścieżki dostosowanej z mapy kampusu PWr.
 *
 * Wymagania: US1.4
 *
 * @property id identyfikator odcinka z danych PWr
 * @property line przebieg odcinka, co najmniej dwa punkty
 */
public data class AccessiblePath(val id: String, val line: List<GeoPoint>)

/**
 * Odcinki ścieżek dostosowanych z atrybucją źródła.
 *
 * @property source atrybucja danych, np. „Politechnika Wrocławska”
 * @property paths odcinki według identyfikatora
 */
public data class AccessiblePathList(val source: String, val paths: List<AccessiblePath>)
