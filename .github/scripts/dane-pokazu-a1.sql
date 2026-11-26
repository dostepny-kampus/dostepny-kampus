-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

-- Budynek A-1 w bazie testu systemowego jak po imporcie danych PWr ze stanem na 01.01.2025: pięć pięter planu,
-- wejście główne z OpenStreetMap niedostosowane i drugie wejście dostosowane. Import PWr pobiera dane z usługi
-- uczelni, której runner CI nie używa, więc test drogi demo dostaje ten stan stąd. Powtórne wykonanie niczego
-- nie zmienia.
UPDATE buildings SET detail_level = 'DETAILED' WHERE symbol = 'A-1';

INSERT INTO floors (building_symbol, id, label, sort_order) VALUES
    ('A-1', 'pokaz-a1-0', 'Parter', 0),
    ('A-1', 'pokaz-a1-1', '1 piętro', 1),
    ('A-1', 'pokaz-a1-2', '2 piętro', 2),
    ('A-1', 'pokaz-a1-3', '3 piętro', 3),
    ('A-1', 'pokaz-a1-4', '4 piętro', 4)
ON CONFLICT DO NOTHING;

UPDATE entrances
SET main = TRUE, pwr_name = 'Wejście główne', pwr_marking = 'INACCESSIBLE', pwr_marking_date = DATE '2025-01-01'
WHERE building_symbol = 'A-1' AND id = 'node/4436524366';

INSERT INTO entrances (id, building_symbol, name, type, main, location, marking, marking_date, marking_source,
    pwr_name, pwr_marking, pwr_marking_date)
VALUES ('pokaz-a1-boczne', 'A-1', NULL, NULL, FALSE, ST_SetSRID(ST_MakePoint(17.0626711, 51.1073101), 4326),
    'NO_DATA', DATE '2025-01-01', 'IMPORT', 'Wejście boczne', 'ACCESSIBLE', DATE '2025-01-01')
ON CONFLICT DO NOTHING;
