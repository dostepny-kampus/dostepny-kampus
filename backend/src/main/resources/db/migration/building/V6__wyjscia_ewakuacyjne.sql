-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

CREATE TABLE evacuation_exits (
    id TEXT NOT NULL,
    building_symbol VARCHAR(16) NOT NULL REFERENCES buildings (symbol),
    floor_id TEXT,
    location GEOMETRY(POINT, 4326) NOT NULL,
    description TEXT NOT NULL,
    door_width_cm INTEGER CONSTRAINT evacuation_exits_door_width_check CHECK (door_width_cm > 0),
    PRIMARY KEY (id, building_symbol)
);

COMMENT ON TABLE evacuation_exits IS 'Wyjścia ewakuacyjne z warstwy wejść PWr; nie są wejściami budynku (US2.6, BR-27)';
COMMENT ON COLUMN evacuation_exits.id IS 'Identyfikator pwr/ z geohashem położenia';
COMMENT ON COLUMN evacuation_exits.description IS 'Uwagi wpisu, np. Wyjście ewakuacyjne z pomieszczenia 41';
