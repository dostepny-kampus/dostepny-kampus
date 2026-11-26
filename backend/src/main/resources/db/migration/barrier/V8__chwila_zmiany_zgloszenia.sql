-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

ALTER TABLE submissions ADD COLUMN changed_at TIMESTAMPTZ NOT NULL DEFAULT statement_timestamp();

CREATE FUNCTION record_submission_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    NEW.changed_at := statement_timestamp();
    RETURN NEW;
END;
$$;

CREATE TRIGGER submissions_change
    BEFORE UPDATE ON submissions
    FOR EACH ROW
    EXECUTE FUNCTION record_submission_change();

CREATE INDEX submissions_changed ON submissions (changed_at);

COMMENT ON COLUMN submissions.changed_at IS 'Chwila ostatniej zmiany wiersza zgłoszenia; kursor pobrania zmian do telefonu';
