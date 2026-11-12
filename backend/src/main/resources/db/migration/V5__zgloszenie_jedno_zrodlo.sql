-- Dostępny Kampus
-- Author: Yevhenii Lukianenko
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

-- Rodzaj, budynek, punkt i piętro zgłoszenia mają jedno źródło: treść JSON (#837).
-- Kolumny liczy baza z treści, więc nie da się ich zapisać osobno ani rozjechać
-- z treścią. Istniejące wiersze dostają wartości z treści, którą czyta serwer.
ALTER TABLE submissions
    DROP COLUMN kind,
    DROP COLUMN building_symbol,
    DROP COLUMN location;

ALTER TABLE submissions
    ADD COLUMN kind VARCHAR(16) NOT NULL
        GENERATED ALWAYS AS (CAST(content ->> 'kind' AS VARCHAR(16))) STORED
        CONSTRAINT submissions_kind_check CHECK (kind IN ('PRECISE', 'QUICK', 'ENTRANCE_MARKING')),
    ADD COLUMN building_symbol VARCHAR(16)
        GENERATED ALWAYS AS (CAST(content ->> 'buildingSymbol' AS VARCHAR(16))) STORED
        REFERENCES buildings (symbol),
    ADD COLUMN location GEOMETRY(POINT, 4326) NOT NULL
        GENERATED ALWAYS AS (
            ST_SetSRID(
                ST_MakePoint(
                    CAST(content #>> '{location,coordinates,0}' AS DOUBLE PRECISION),
                    CAST(content #>> '{location,coordinates,1}' AS DOUBLE PRECISION)
                ),
                4326
            )
        ) STORED,
    ADD COLUMN floor_id TEXT
        GENERATED ALWAYS AS (content #>> '{floor,floorId}') STORED;

CREATE INDEX submissions_building_symbol ON submissions (building_symbol);
CREATE INDEX submissions_building_floor ON submissions (building_symbol, floor_id);

COMMENT ON COLUMN submissions.kind IS 'PRECISE: pomiar precyzyjny, QUICK: zgłoszenie szybkie, ENTRANCE_MARKING: zmiana oznaczenia wejścia; z treści (kind)';
COMMENT ON COLUMN submissions.building_symbol IS 'Budynek zgłoszenia; brak to „poza budynkiem” (BR-G2, BR-14); z treści (buildingSymbol)';
COMMENT ON COLUMN submissions.location IS 'Miejsce zgłoszenia w EPSG:4326; z treści (location.coordinates)';
COMMENT ON COLUMN submissions.floor_id IS 'Piętro zgłoszenia dla filtra listy (US8); z treści (floor.floorId)';
