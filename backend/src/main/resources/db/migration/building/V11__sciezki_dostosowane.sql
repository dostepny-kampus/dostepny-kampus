-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

CREATE TABLE accessible_paths (
    id TEXT PRIMARY KEY,
    line GEOMETRY(LINESTRING, 4326) NOT NULL
);

COMMENT ON TABLE accessible_paths IS 'Odcinki ścieżek dostosowanych z mapy kampusu PWr (US1.4, NFR-40)';
COMMENT ON COLUMN accessible_paths.id IS 'Identyfikator odcinka z danych PWr';
COMMENT ON COLUMN accessible_paths.line IS 'Przebieg odcinka w WGS84';
