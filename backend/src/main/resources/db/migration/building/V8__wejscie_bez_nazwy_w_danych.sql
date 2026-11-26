-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

-- Zastępczą nazwę „Wejście” dokłada odczyt (BR-27); import zapisuje brak nazwy.
UPDATE entrances SET pwr_name = NULL WHERE pwr_name = 'Wejście';
UPDATE entrances SET name = NULL WHERE id LIKE 'pwr/%' AND name = 'Wejście';

COMMENT ON COLUMN entrances.pwr_name IS 'Nazwa z uwag warstwy wejść PWr (BR-27); wygrywa z name z OpenStreetMap, NULL poza warstwą i bez nazwy w uwagach';
