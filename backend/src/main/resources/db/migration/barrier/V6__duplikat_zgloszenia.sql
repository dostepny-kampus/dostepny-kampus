-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

ALTER TABLE submissions ADD COLUMN duplicate_of UUID;

COMMENT ON COLUMN submissions.duplicate_of IS 'Wcześniejsze zgłoszenie wskazane przy odrzuceniu duplikatu (BR-13)';
