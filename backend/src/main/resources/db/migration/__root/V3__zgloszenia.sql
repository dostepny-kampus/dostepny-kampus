-- Dostępny Kampus
-- Author: Myroslav Hryniuka
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

CREATE TABLE submissions (
    id UUID PRIMARY KEY,
    version INTEGER NOT NULL DEFAULT 1
        CONSTRAINT submissions_version_check CHECK (version >= 1),
    status VARCHAR(16) NOT NULL DEFAULT 'SENT'
        CONSTRAINT submissions_status_check
            CHECK (status IN ('SENT', 'IN_REVIEW', 'ACCEPTED', 'REJECTED', 'ARCHIVED')),
    kind VARCHAR(16) NOT NULL
        CONSTRAINT submissions_kind_check CHECK (kind IN ('PRECISE', 'QUICK', 'ENTRANCE_MARKING')),
    building_symbol VARCHAR(16) REFERENCES buildings (symbol),
    location GEOMETRY(POINT, 4326) NOT NULL,
    content JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX submissions_building_symbol ON submissions (building_symbol);
CREATE INDEX submissions_status ON submissions (status);

COMMENT ON TABLE submissions IS 'Zgłoszenia barier i zmian oznaczenia wejścia (US4, US7, US8, US17.2)';
COMMENT ON COLUMN submissions.id IS 'Identyfikator nadany w telefonie; ponowne wysłanie nie tworzy drugiego zgłoszenia (BR-9)';
COMMENT ON COLUMN submissions.version IS 'Wersja zgłoszenia; rośnie po każdej zapisanej edycji (BR-3, BR-10)';
COMMENT ON COLUMN submissions.status IS 'Status zgłoszenia na serwerze (US8.1, US8.2)';
COMMENT ON COLUMN submissions.kind IS 'PRECISE: pomiar precyzyjny, QUICK: zgłoszenie szybkie, ENTRANCE_MARKING: zmiana oznaczenia wejścia';
COMMENT ON COLUMN submissions.building_symbol IS 'Budynek zgłoszenia; brak to „poza budynkiem” (BR-G2, BR-14)';
COMMENT ON COLUMN submissions.location IS 'Miejsce zgłoszenia w EPSG:4326';
COMMENT ON COLUMN submissions.content IS 'Treść zgłoszenia według schematu SubmissionContent z kontraktu API';
COMMENT ON COLUMN submissions.created_at IS 'Chwila odbioru zgłoszenia przez serwer';
