-- V4 — convert all native ENUM columns to VARCHAR
-- Hibernate 6 with @Enumerated(EnumType.STRING) or plain String fields
-- binds Java strings which PostgreSQL rejects for native ENUM columns.
-- App-level enums and validation replace the DB-level type checking.

ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(50);
DROP TYPE IF EXISTS user_role;

ALTER TABLE audio_assessments ALTER COLUMN status TYPE VARCHAR(50);
DROP TYPE IF EXISTS assessment_status;

ALTER TABLE staging_assessments ALTER COLUMN status TYPE VARCHAR(50);
DROP TYPE IF EXISTS staging_status;

ALTER TABLE pending_audio_jobs ALTER COLUMN status TYPE VARCHAR(50);
DROP TYPE IF EXISTS job_status;

ALTER TABLE assessment_appeals ALTER COLUMN status TYPE VARCHAR(50);
DROP TYPE IF EXISTS appeal_status;
