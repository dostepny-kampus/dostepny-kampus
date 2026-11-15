-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

ALTER TABLE submissions ADD COLUMN reviewer VARCHAR(255);

COMMENT ON COLUMN submissions.reviewer IS 'Identyfikator konta Weryfikatora, który wziął zgłoszenie do weryfikacji (BR-11)';
