-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

CREATE TABLE entrances (
    id TEXT NOT NULL,
    building_symbol VARCHAR(16) NOT NULL REFERENCES buildings (symbol),
    name TEXT,
    type TEXT,
    main BOOLEAN NOT NULL,
    location GEOMETRY(POINT, 4326) NOT NULL,
    marking VARCHAR(16) NOT NULL
        CONSTRAINT entrances_marking_check CHECK (marking IN ('ACCESSIBLE', 'INACCESSIBLE', 'NO_DATA')),
    marking_date DATE NOT NULL,
    marking_source VARCHAR(16) NOT NULL
        CONSTRAINT entrances_marking_source_check CHECK (marking_source IN ('IMPORT', 'WALKTHROUGH')),
    PRIMARY KEY (id, building_symbol)
);

CREATE INDEX entrances_building_symbol ON entrances (building_symbol);

COMMENT ON TABLE entrances IS 'Wejścia budynków z oznaczeniem wejścia (US2, US17, BR-24)';
COMMENT ON COLUMN entrances.id IS 'Identyfikator wejścia, np. node/1167953448 z OpenStreetMap';
COMMENT ON COLUMN entrances.building_symbol IS 'Budynek; wejście wspólne kilku budynków ma wiersz dla każdego z nich';
COMMENT ON COLUMN entrances.name IS 'Opis wejścia, np. W-1; NULL, gdy dane go nie podają';
COMMENT ON COLUMN entrances.type IS 'Rodzaj wejścia z tagu entrance, np. main, service, emergency';
COMMENT ON COLUMN entrances.marking IS 'Oznaczenie wejścia: dostępne, niedostępne albo brak danych (BR-24)';
COMMENT ON COLUMN entrances.marking_date IS 'Dzień importu albo przyjęcia zmiany oznaczenia (US17.3)';
COMMENT ON COLUMN entrances.marking_source IS 'IMPORT: dane OSM albo PWr, WALKTHROUGH: obchód; import nie zmienia oznaczenia z obchodu';
