/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.export

/**
 * Format pliku eksportu bieżących zgłoszeń przyjętych.
 *
 * Wymagania: US13.1, US13.2
 *
 * @property extension rozszerzenie pliku, także w ścieżce trasy eksportu
 * @property mediaType typ treści pliku
 */
public enum class ExportFormat(public val extension: String, public val mediaType: String) {
    /** GeoJSON w WGS84. Wymagania: US13.1 */
    GEOJSON(extension = "geojson", mediaType = "application/geo+json"),

    /** CSV w PUWG 1992 (EPSG:2180). Wymagania: US13.2 */
    CSV(extension = "csv", mediaType = "text/csv"),
}
