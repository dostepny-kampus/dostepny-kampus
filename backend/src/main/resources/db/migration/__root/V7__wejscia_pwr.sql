-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

ALTER TABLE entrances ADD COLUMN approach TEXT;
ALTER TABLE entrances ADD COLUMN door_width_cm INTEGER
    CONSTRAINT entrances_door_width_check CHECK (door_width_cm > 0);
ALTER TABLE entrances ADD COLUMN pwr BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN entrances.approach IS 'Opis dojścia z uwag warstwy wejść PWr (BR-27); NULL, gdy dane go nie podają';
COMMENT ON COLUMN entrances.door_width_cm IS 'Szerokość drzwi w centymetrach z warstwy wejść PWr; NULL zamiast 0 (BR-27)';
COMMENT ON COLUMN entrances.pwr IS 'Wejście jest w warstwie wejść PWr: nazwę, opis, drzwi i oznaczenie z importu daje PWr, a import OpenStreetMap ich nie zmienia';
