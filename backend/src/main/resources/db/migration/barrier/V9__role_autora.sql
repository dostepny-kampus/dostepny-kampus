-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

ALTER TABLE submissions ADD COLUMN author_roles TEXT[] NOT NULL DEFAULT '{}';

COMMENT ON COLUMN submissions.author_roles IS 'Role autora z tokenu w chwili pierwszego wysłania (BR-5); puste dla zgłoszeń sprzed zapisu ról';
