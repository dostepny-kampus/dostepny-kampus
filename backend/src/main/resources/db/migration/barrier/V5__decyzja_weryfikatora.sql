-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

ALTER TABLE submissions ADD COLUMN rejection_reason VARCHAR(500);

COMMENT ON COLUMN submissions.rejection_reason IS 'Powód odrzucenia podany przez Weryfikatora (BR-16)';
