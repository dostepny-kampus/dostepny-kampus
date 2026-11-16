-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

ALTER TABLE submissions ADD COLUMN rejected_at TIMESTAMPTZ;

UPDATE submissions SET rejected_at = now() WHERE status = 'REJECTED';

CREATE FUNCTION record_submission_rejection() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    NEW.rejected_at := statement_timestamp();
    RETURN NEW;
END;
$$;

CREATE TRIGGER submissions_rejection
    BEFORE UPDATE OF status ON submissions
    FOR EACH ROW
    WHEN (NEW.status = 'REJECTED' AND OLD.status IS DISTINCT FROM NEW.status)
    EXECUTE FUNCTION record_submission_rejection();

CREATE INDEX submissions_rejected_at ON submissions (rejected_at) WHERE status = 'REJECTED';

COMMENT ON COLUMN submissions.rejected_at IS 'Chwila ostatniego odrzucenia; od niej liczy się 90 dni do usunięcia (NFR-29)';
