-- V4 — convert all native ENUM columns to VARCHAR
-- PostgreSQL won't let us drop a type if anything references it
-- (e.g. default values). ALTER the columns first, then CASCADE the types.

ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(50);
ALTER TABLE audio_assessments ALTER COLUMN status TYPE VARCHAR(50);
ALTER TABLE staging_assessments ALTER COLUMN status TYPE VARCHAR(50);
ALTER TABLE pending_audio_jobs ALTER COLUMN status TYPE VARCHAR(50);
ALTER TABLE assessment_appeals ALTER COLUMN status TYPE VARCHAR(50);

-- Now drop the unused enum types (CASCADE handles defaults and casts)
DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS assessment_status CASCADE;
DROP TYPE IF EXISTS staging_status CASCADE;
DROP TYPE IF EXISTS job_status CASCADE;
DROP TYPE IF EXISTS appeal_status CASCADE;
