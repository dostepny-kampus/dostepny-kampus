-- Dostępny Kampus
-- Author: Yevhenii Lukianenko
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

-- Powiązanie zmiany z przyjętym zgłoszeniem, które ona zastąpi (BR-18, BR-19). Kolumna
-- wynika z treści jak pozostałe kolumny zgłoszenia, więc wiersze zapisane wcześniej dostają
-- ją przy migracji. Klucza obcego nie ma: zastępowane zgłoszenie serwer sprawdza przy
-- zapisie, a wiersz sprzed tej kontroli może wskazywać zgłoszenie, którego nie ma.
ALTER TABLE submissions
    ADD COLUMN replaces_submission_id UUID
        GENERATED ALWAYS AS (CAST(content ->> 'replacesSubmissionId' AS UUID)) STORED;

CREATE INDEX submissions_replaces ON submissions (replaces_submission_id)
    WHERE replaces_submission_id IS NOT NULL;

COMMENT ON COLUMN submissions.replaces_submission_id IS 'Przyjęte zgłoszenie, które ta zmiana zastąpi po przyjęciu (BR-18); z treści (replacesSubmissionId)';
