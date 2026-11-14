-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

-- Symbol budynku w zgłoszeniu to zwykła wartość: miejsce zgłoszenia w chwili zapisu.
-- Budynek sprawdza serwer przy zapisie przez API modułu budynków, a tabela budynków
-- należy do innego modułu, więc klucz obcy do niej znika.
ALTER TABLE submissions DROP CONSTRAINT submissions_building_symbol_fkey;
