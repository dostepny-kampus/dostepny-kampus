-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE buildings (
    symbol VARCHAR(16) PRIMARY KEY,
    name TEXT NOT NULL,
    detail_level VARCHAR(8) NOT NULL DEFAULT 'BASIC'
        CONSTRAINT buildings_detail_level_check CHECK (detail_level IN ('DETAILED', 'BASIC')),
    outline GEOMETRY(MULTIPOLYGON, 4326) NOT NULL,
    source TEXT NOT NULL
);

CREATE INDEX buildings_outline_gist ON buildings USING GIST (outline);

COMMENT ON TABLE buildings IS 'Budynki PWr na mapie kampusu (US1, US2)';
COMMENT ON COLUMN buildings.symbol IS 'Symbol budynku z dywizem, np. C-13';
COMMENT ON COLUMN buildings.name IS 'Nazwa budynku';
COMMENT ON COLUMN buildings.detail_level IS 'DETAILED: budynek z planami pięter PWr, BASIC: budynek podstawowy';
COMMENT ON COLUMN buildings.outline IS 'Kontur budynku w EPSG:4326; kilka części budynku to jeden wielokąt złożony';
COMMENT ON COLUMN buildings.source IS 'Atrybucja źródła konturu, np. © OpenStreetMap contributors';
