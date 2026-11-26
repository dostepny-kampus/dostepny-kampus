-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

ALTER TABLE entrances ADD COLUMN walkthrough_at TIMESTAMPTZ;

COMMENT ON COLUMN entrances.walkthrough_at IS
    'Chwila przyjęcia najnowszego zgłoszenia zmiany oznaczenia wejścia (BR-23)';
