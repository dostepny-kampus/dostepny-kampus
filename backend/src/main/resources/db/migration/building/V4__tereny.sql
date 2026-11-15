-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

CREATE TABLE campus_areas (
    name TEXT PRIMARY KEY,
    boundary GEOMETRY(MULTIPOLYGON, 4326) NOT NULL
);

CREATE INDEX campus_areas_boundary_gist ON campus_areas USING GIST (boundary);

COMMENT ON TABLE campus_areas IS 'Granice pięciu terenów PWr z zapasem; zgłoszenie przyjmuje serwer tylko wewnątrz któregoś (US4.9, BR-G5)';
COMMENT ON COLUMN campus_areas.name IS 'Nazwa terenu, np. Kampus główny';
COMMENT ON COLUMN campus_areas.boundary IS 'Granica terenu w EPSG:4326 z pliku data/areas/pwr-areas.geojson; punkt na linii należy do terenu';
