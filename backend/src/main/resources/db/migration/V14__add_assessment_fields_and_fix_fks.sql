-- V14 — add LLM output fields to assessment_records, add full_transcript to staging,
-- and fix FK column names to match JPA entity mappings (record_id not assessment_record_id)

-- 1. Add new columns to assessment_records for LLM output
ALTER TABLE assessment_records ADD COLUMN IF NOT EXISTS rating_level VARCHAR(50);
ALTER TABLE assessment_records ADD COLUMN IF NOT EXISTS confidence VARCHAR(20);
ALTER TABLE assessment_records ADD COLUMN IF NOT EXISTS evidence TEXT;

-- 2. Add full transcript storage to staging_assessments (currently only stores 200-char snippet)
ALTER TABLE staging_assessments ADD COLUMN IF NOT EXISTS full_transcript TEXT;

-- 3. Fix feedback FK column: DB has assessment_record_id but JPA entity maps record_id
-- Drop the old FK constraint first, then rename the column, then re-add the constraint
ALTER TABLE feedback DROP CONSTRAINT IF EXISTS feedback_assessment_record_id_fkey;
ALTER TABLE feedback RENAME COLUMN assessment_record_id TO record_id;
ALTER TABLE feedback ADD CONSTRAINT fk_feedback_record
    FOREIGN KEY (record_id) REFERENCES assessment_records(record_id) ON DELETE CASCADE;

-- 4. Fix assessment_versions FK column: same mismatch
ALTER TABLE assessment_versions DROP CONSTRAINT IF EXISTS assessment_versions_assessment_record_id_fkey;
ALTER TABLE assessment_versions RENAME COLUMN assessment_record_id TO record_id;
ALTER TABLE assessment_versions ADD CONSTRAINT fk_versions_record
    FOREIGN KEY (record_id) REFERENCES assessment_records(record_id) ON DELETE CASCADE;
