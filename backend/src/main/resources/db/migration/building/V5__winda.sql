-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

-- Winda łącząca piętra (#1919): identyfikator `<piętro>/<pomieszczenie>` pomieszczenia windy
-- na najniższym piętrze szybu, wspólny dla pomieszczeń windy z kolejnych pięter; NULL poza
-- windami. Piętra windy to piętra pomieszczeń z tym samym identyfikatorem.
ALTER TABLE rooms ADD COLUMN lift_id TEXT;
