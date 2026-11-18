-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

ALTER TABLE entrances ADD COLUMN pwr_marking_date DATE;

COMMENT ON COLUMN entrances.pwr_marking_date IS 'Dzień ostatniej edycji warstwy wejść PWr, data oznaczenia pwr_marking (BR-24, US17.3); NULL poza warstwą';
