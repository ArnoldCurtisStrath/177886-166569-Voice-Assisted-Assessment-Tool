-- V16: Add resolution_note to assessment_appeals
-- When a teacher resolves an appeal (upheld/adjusted/dismissed),
-- they should be able to leave an explanation the student can see.

ALTER TABLE assessment_appeals
    ADD COLUMN IF NOT EXISTS resolution_note TEXT;
