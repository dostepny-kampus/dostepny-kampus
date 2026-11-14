-- Dostępny Kampus
-- Author: Yevhenii Lukianenko
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

-- Znacznik nie ma klucza do floors: import planów PWr usuwa i tworzy piętra,
-- a znacznik ma przetrwać import.
CREATE TABLE inspection_markers (
    id UUID PRIMARY KEY,
    building_symbol VARCHAR(16) NOT NULL,
    floor_id TEXT,
    author VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ
);

CREATE INDEX inspection_markers_active ON inspection_markers (building_symbol)
WHERE revoked_at IS NULL;

COMMENT ON TABLE inspection_markers IS 'Znaczniki pięter obejrzanych przez Zbierających (US15, BR-20, BR-21)';
COMMENT ON COLUMN inspection_markers.id IS 'Identyfikator znacznika nadany w telefonie';
COMMENT ON COLUMN inspection_markers.building_symbol IS 'Symbol budynku znacznika';
COMMENT ON COLUMN inspection_markers.floor_id IS 'Piętro obejrzane w budynku szczegółowym; NULL, gdy znacznik dotyczy całego budynku podstawowego';
COMMENT ON COLUMN inspection_markers.author IS 'Identyfikator konta (sub z tokenu) osoby, która postawiła znacznik';
COMMENT ON COLUMN inspection_markers.created_at IS 'Chwila odbioru znacznika przez serwer';
COMMENT ON COLUMN inspection_markers.revoked_at IS 'Chwila cofnięcia znacznika; NULL, gdy znacznik jest aktywny';
