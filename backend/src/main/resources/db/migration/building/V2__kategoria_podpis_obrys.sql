-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

-- Dane planu liczone raz przy imporcie (US16.1, US18.1): grupa funkcji pomieszczenia, punkt
-- podpisu numeru, znacznik podpisanego numeru i obrys piętra. Wiersze zapisane wcześniej dostają
-- kategorię „OTHER”, punkt wewnątrz konturu i brak podpisu do następnego importu.
ALTER TABLE rooms
    ADD COLUMN category TEXT NOT NULL DEFAULT 'OTHER',
    ADD COLUMN label_point GEOMETRY(POINT, 4326),
    ADD COLUMN labelled BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE rooms SET label_point = ST_PointOnSurface(outline);

ALTER TABLE rooms ALTER COLUMN label_point SET NOT NULL;

ALTER TABLE floors ADD COLUMN outline GEOMETRY(MULTIPOLYGON, 4326);

CREATE INDEX rooms_category ON rooms (category);

COMMENT ON COLUMN rooms.category IS 'Grupa funkcji pomieszczenia (RoomCategory z kontraktu API)';
COMMENT ON COLUMN rooms.label_point IS 'Punkt wewnątrz pomieszczenia na podpis numeru, EPSG:4326';
COMMENT ON COLUMN rooms.labelled IS 'Czy numer pomieszczenia jest podpisany na planie piętra';
COMMENT ON COLUMN floors.outline IS 'Obrys piętra: suma pomieszczeń piętra, EPSG:4326';
