-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

CREATE TABLE floors (
    building_symbol VARCHAR(16) NOT NULL REFERENCES buildings (symbol),
    id TEXT NOT NULL,
    label TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (building_symbol, id)
);

CREATE INDEX floors_building_order ON floors (building_symbol, sort_order, id);

CREATE TABLE rooms (
    building_symbol VARCHAR(16) NOT NULL,
    floor_id TEXT NOT NULL,
    id TEXT NOT NULL,
    number TEXT,
    function TEXT NOT NULL,
    outline GEOMETRY(MULTIPOLYGON, 4326) NOT NULL,
    PRIMARY KEY (building_symbol, floor_id, id),
    FOREIGN KEY (building_symbol, floor_id) REFERENCES floors (building_symbol, id)
);

CREATE INDEX rooms_number ON rooms (number);

COMMENT ON TABLE floors IS 'Piętra budynków z planów Politechniki Wrocławskiej (US16)';
COMMENT ON COLUMN floors.building_symbol IS 'Symbol budynku, do którego należy piętro';
COMMENT ON COLUMN floors.id IS 'Identyfikator piętra w obrębie budynku';
COMMENT ON COLUMN floors.label IS 'Etykieta piętra z danych PWr pokazywana w przełączniku pięter';
COMMENT ON COLUMN floors.sort_order IS 'Kolejność piętra od najniższego do najwyższego';

COMMENT ON TABLE rooms IS 'Pomieszczenia z planów Politechniki Wrocławskiej (US16, US18)';
COMMENT ON COLUMN rooms.building_symbol IS 'Symbol budynku zawierającego pomieszczenie';
COMMENT ON COLUMN rooms.floor_id IS 'Identyfikator piętra w obrębie budynku';
COMMENT ON COLUMN rooms.id IS 'Identyfikator pomieszczenia w obrębie piętra';
COMMENT ON COLUMN rooms.number IS 'Numer do wyszukiwania pomieszczenia, NULL przy braku numeru w danych PWr';
COMMENT ON COLUMN rooms.function IS 'Funkcja pomieszczenia z danych PWr';
COMMENT ON COLUMN rooms.outline IS 'Kontur pomieszczenia w EPSG:4326';
