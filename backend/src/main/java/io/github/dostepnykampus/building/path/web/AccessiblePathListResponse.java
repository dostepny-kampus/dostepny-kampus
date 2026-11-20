/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path.web;

import com.fasterxml.jackson.annotation.JsonRawValue;
import io.github.dostepnykampus.building.domain.FloorList;
import io.github.dostepnykampus.building.path.AccessiblePath;
import java.util.List;

/**
 * Odcinki ścieżek dostosowanych w odpowiedzi HTTP, zgodne ze schematem AccessiblePathList.
 *
 * <p>Wymagania: US1.4, NFR-40</p>
 *
 * @param source Nazwa źródła danych PWr
 * @param paths Odcinki według identyfikatora
 * @since 0.1.0
 */
public record AccessiblePathListResponse(String source, List<Path> paths) {

    /**
     * Odpowiedź z odcinków.
     *
     * @param paths Odcinki według identyfikatora
     */
    public AccessiblePathListResponse(final List<AccessiblePath> paths) {
        this(FloorList.PWR_SOURCE, paths.stream().map(Path::new).toList());
    }

    /**
     * Odcinek w odpowiedzi HTTP, zgodny ze schematem AccessiblePath.
     *
     * @param id Identyfikator odcinka
     * @param line Przebieg odcinka jako GeoJSON
     * @since 0.1.0
     */
    public record Path(String id, @JsonRawValue String line) {

        /**
         * Odpowiedź z odcinka.
         *
         * @param path Odcinek ścieżki dostosowanej
         */
        public Path(final AccessiblePath path) {
            this(path.id(), path.line());
        }
    }
}
