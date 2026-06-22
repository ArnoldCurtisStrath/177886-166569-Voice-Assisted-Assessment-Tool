-- V6 — add school_id to subjects (was missing from V1 schema)
-- The Java entity Subject requires school_id NOT NULL but the SQL table never had it.
-- This fixes that mismatch so JPA queries don't crash.

ALTER TABLE subjects ADD COLUMN school_id UUID;

-- backfill existing subjects to the seed school
UPDATE subjects SET school_id = 'a1000000-0000-0000-0000-000000000001'
WHERE school_id IS NULL;

ALTER TABLE subjects ALTER COLUMN school_id SET NOT NULL;

ALTER TABLE subjects ADD CONSTRAINT fk_subjects_school
    FOREIGN KEY (school_id) REFERENCES schools(school_id) ON DELETE RESTRICT;
