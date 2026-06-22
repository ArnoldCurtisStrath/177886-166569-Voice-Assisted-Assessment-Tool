-- V5 — convert JSONB columns to TEXT
-- Hibernate binds Java strings which PostgreSQL rejects for JSONB.
-- We store JSON strings, not structured objects, so TEXT is fine.

ALTER TABLE administrators ALTER COLUMN capability_array TYPE TEXT;
ALTER TABLE audio_assessments ALTER COLUMN target_sub_strands TYPE TEXT;
