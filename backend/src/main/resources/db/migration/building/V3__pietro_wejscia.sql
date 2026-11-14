-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

-- Piętro wejścia z warstwy wejść PWr (US18.2, piętro planu na start): identyfikator z listy pięter
-- budynku albo NULL, gdy dane go nie podają. Bez klucza obcego: import planu odtwarza piętra
-- i ustawia piętra wejść od nowa.
ALTER TABLE entrances ADD COLUMN floor_id TEXT;
