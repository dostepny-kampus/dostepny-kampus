-- Dostępny Kampus
-- Author: Vladyslav Yevkov
-- Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

ALTER TABLE submissions ADD COLUMN verified_at TIMESTAMPTZ;

CREATE FUNCTION record_submission_verification() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    NEW.verified_at := statement_timestamp();
    RETURN NEW;
END;
$$;
CREATE TRIGGER submissions_verification
    BEFORE UPDATE OF status ON submissions
    FOR EACH ROW
    WHEN (NEW.status = 'ACCEPTED' AND OLD.status IS DISTINCT FROM NEW.status)
    EXECUTE FUNCTION record_submission_verification();
