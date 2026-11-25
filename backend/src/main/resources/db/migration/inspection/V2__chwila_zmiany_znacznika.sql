-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

ALTER TABLE inspection_markers ADD COLUMN changed_at TIMESTAMPTZ NOT NULL DEFAULT now();
UPDATE inspection_markers SET changed_at = created_at;

CREATE INDEX inspection_markers_changed ON inspection_markers (changed_at);

COMMENT ON COLUMN inspection_markers.changed_at IS 'Chwila ostatniej zmiany znacznika (postawienie albo cofnięcie); kursor pobrania zmian do telefonu';
