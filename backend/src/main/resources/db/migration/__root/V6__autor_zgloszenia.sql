-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

-- Autor zgłoszenia: identyfikator konta z tokenu (BR-3, US8). Zgłoszenia zapisane
-- przed tą zmianą zostają bez autora.
ALTER TABLE submissions ADD COLUMN author VARCHAR(255);

CREATE INDEX submissions_author ON submissions (author);

COMMENT ON COLUMN submissions.author IS 'Identyfikator konta (sub z tokenu) osoby, która złożyła zgłoszenie; brak w zgłoszeniach sprzed logowania';
