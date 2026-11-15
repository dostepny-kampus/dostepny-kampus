-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

CREATE TABLE inspection_markers (
    id UUID PRIMARY KEY,
    building_symbol VARCHAR(16) NOT NULL,
    floor_id TEXT,
    author VARCHAR(255) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX inspection_markers_building ON inspection_markers (building_symbol);

COMMENT ON TABLE inspection_markers IS 'Znaczniki pięter obejrzanych przez Zbierających (US15.1, US15.2, BR-20)';
COMMENT ON COLUMN inspection_markers.id IS 'Identyfikator nadany w telefonie; ponowne wysłanie nie tworzy drugiego znacznika (BR-9)';
COMMENT ON COLUMN inspection_markers.building_symbol IS 'Symbol budynku znacznika; bez klucza obcego, bo budynki należą do innego modułu';
COMMENT ON COLUMN inspection_markers.floor_id IS 'Piętro obejrzane w budynku szczegółowym; NULL w budynku podstawowym, gdzie znacznik dotyczy całego budynku';
COMMENT ON COLUMN inspection_markers.author IS 'Identyfikator konta (sub z tokenu) Zbierającego, który postawił znacznik';
COMMENT ON COLUMN inspection_markers.revoked IS 'Czy znacznik cofnięto (US11.3, US15.2)';
COMMENT ON COLUMN inspection_markers.created_at IS 'Chwila odbioru znacznika przez serwer';
